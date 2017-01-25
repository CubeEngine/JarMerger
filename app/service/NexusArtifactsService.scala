package service

import java.io.{File, FileOutputStream}
import java.nio.file.Path
import javax.inject.Inject

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.Configuration
import play.api.libs.ws.{StreamedResponse, WSClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Node


class NexusArtifactsService @Inject()(wSClient: WSClient, conf: Configuration, implicit private val mat: Materializer) {

    def nexusBase = conf.getString("nexus.base").get

    def findArtifactsIn(repo: String, group: String): Future[Seq[Artifact]] = {

        def extractMetadata(root: Node): ModuleMetadata = {
            val name = (root \ "name").head.child.head.toString
            val desc = (root \ "description").head.child.head.toString
            ModuleMetadata(name, desc)
        }

        def nodeToArtifact(n: Node): Future[Artifact] = {
            val groupId = (n \ "groupId").head.child.head.toString()
            val artifactId = (n \ "artifactId").head.child.head.toString()
            val version = (n \ "version").head.child.head.toString()


            buildFileRequest(repo, groupId, artifactId, version, "pom")
                .get().map(r => extractMetadata(r.xml))
                .map(m => Artifact(groupId, artifactId, version, m))
        }


        def has(t: String)(n: Node): Boolean = {
            (n \ "artifactHits" \ "artifactHit" \ "artifactLinks" \ "artifactLink" \ "extension")
                .exists(_.child.head.toString() == t)
        }

        wSClient
            .url(s"$nexusBase/service/local/lucene/search")
            .withQueryString("r" -> repo, "g" -> group)
            .get().flatMap { response =>
                Future.sequence((response.xml \ "data" \ "artifact")
                    .filter(has("jar"))
                    .map(nodeToArtifact))
            }
    }

    private def buildFileRequest(repo: String, group: String, id: String, version: String, packaging: String = "jar") = {
        wSClient
            .url(s"$nexusBase/service/local/artifact/maven/redirect")
            .withQueryString("r" -> repo, "g" -> group, "a" -> id, "v" -> version, "p" -> packaging)
    }

    def retrieveLatestVersion(to: Path, repo: String, group: String, artifact: String, version: String = "LATEST"): Future[File] = {

        def streamedResponseToFile(file: File)(r: StreamedResponse): Future[File] = {
            val outputStream = new FileOutputStream(file)
            val sink = Sink.foreach[ByteString] {bytes =>
                outputStream.write(bytes.toArray)
            }
            r.body.runWith(sink).andThen {
                case result =>
                    outputStream.close()
                    result.get
            }.map(_ => file)
        }

        val file = to.toFile

        buildFileRequest(repo, group, artifact, "LATEST")
            .withMethod("GET")
            .stream().flatMap(streamedResponseToFile(file))
    }

}
