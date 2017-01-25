name := "JarMergerWS"

version := "1.0"

lazy val `jarmergerws` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    jdbc,
    cache,
    ws,
    specs2 % Test,
    "org.webjars" % "jquery" % "3.1.1"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
