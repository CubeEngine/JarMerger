package controllers

import javax.inject.Inject

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import service.{ModuleBuilderService, NexusArtifactsService}

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(conf: Configuration, moduleBuilder: ModuleBuilderService, nexusArtifacts: NexusArtifactsService) extends Controller {

    private val repo = conf.getString("nexus.repo").get

    def page() = Action {
        Ok(views.html.index())
    }

    def index(group: String) = Action.async {
        nexusArtifacts.findArtifactsIn(repo, group).map {artifacts =>
            val json = Json.toJson(artifacts)
            if (artifacts.nonEmpty) Ok(json)
            else NotFound(json)
        }
    }

    def artifacts(group: String) = Action.async {
        nexusArtifacts.findArtifactsIn(repo, group).map(r => Ok(Json.toJson(r)))
    }

    def artifact(group: String, artifact: String) = Action.async {
        nexusArtifacts.findArtifactsIn(repo, group).map(r => Ok(Json.toJson(r)))
    }

    def getMergedModule(group: String, module: Seq[String]) = Action.async {
        val modules = if (module.nonEmpty) module else moduleBuilder.allModules
        moduleBuilder.buildSuperModuleWith(repo, group, modules).map {superModuleFile =>
            Ok.sendFile(superModuleFile, fileName = _ => modules.mkString("-") + ".jar")
        }
    }
}