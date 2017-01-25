package service

import play.api.libs.json.Json

object ModuleMetadata {
    implicit val format = Json.format[ModuleMetadata]
}
case class ModuleMetadata(name: String, description: String)

object Artifact {
    implicit val format = Json.format[Artifact]
}
case class Artifact(group: String, id: String, version: String, metadata: ModuleMetadata)
