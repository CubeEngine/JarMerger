package service

import java.io._
import java.nio.file.{Files, Path}
import java.util.jar.{JarFile, JarOutputStream}
import java.util.stream.Collectors.toList
import java.util.zip.ZipEntry
import javax.inject.Inject

import play.api.Configuration

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ModuleBuilderService @Inject()(conf: Configuration, nexus: NexusArtifactsService) {

    private def modulesPath: Path = Util.folderOrDefault(conf, "modules-path", "modules")

    private val tempPath = Util.tempDir(conf)

    def allModules: Seq[String] = {
        Files.list(modulesPath).collect(toList()).toList.filter(_.toString.endsWith(".jar")).map { path =>
            val n = path.getFileName.toString
            n.substring(0, n.lastIndexOf('.'))
        }
    }

    def buildSuperModuleWith(repo: String, group: String, modules: Seq[String]): Future[File] = {
        val name = Util.md5(repo + group + modules.sorted.mkString(","))

        val superModule: File = tempPath.resolve(s"$name.jar").toFile
        if (superModule.exists()) {
            Future.successful(superModule)
        } else {
            val downloaded = modules.map(m => nexus.retrieveLatestVersion(tempPath.resolve(s"${name}_$m.jar"), repo, group, m))
            Future.sequence(downloaded) map {files =>
                val jarOutput = new JarOutputStream(new FileOutputStream(superModule))
                val existingEntries = mutable.Set[String]()

                for (moduleFile <- files) {
                    val jarInput = new JarFile(moduleFile, true)
                    jarInput.stream().collect(toList()).toList.foreach { e =>
                        val entryName = e.getName

                        if (!existingEntries.contains(entryName)) {
                            existingEntries.add(entryName)
                            jarOutput.putNextEntry(new ZipEntry(entryName))
                            if (!e.isDirectory) {
                                val reader = jarInput.getInputStream(e)
                                try {
                                    val BufSize = 512
                                    val buf = Array.ofDim[Byte](BufSize)
                                    var bytesRead = -1
                                    do {
                                        bytesRead = reader.read(buf, 0, BufSize)
                                        if (bytesRead != -1) {
                                            jarOutput.write(buf, 0, bytesRead)
                                        }
                                    } while (bytesRead != -1)
                                } finally {
                                    reader.close()
                                }
                            }
                            jarOutput.closeEntry()
                        }
                    }
                    jarInput.close()
                }
                jarOutput.close()

                superModule
            }
        }
    }
}
