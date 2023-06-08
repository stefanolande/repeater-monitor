val scala3Version = "3.2.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "repeater-monitor",
    version := "0.1.0-SNAPSHOT",
    assembly / assemblyJarName := "repeater-monitor.jar",
    scalaVersion := scala3Version
  )

val http4sVersion          = "0.23.19"
val logbackVersion         = "1.4.6"
val munitCatsEffectVersion = "2.0.0-M3"
val circeVersion           = "0.14.5"
val pureconfigVersion      = "0.17.2"

libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "0.7.29" % Test,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.typelevel" %% "munit-cats-effect" % munitCatsEffectVersion % "test",
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion
)

//ThisBuild / assemblyMergeStrategy := {
//  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
//  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
//  case "application.conf"                            => MergeStrategy.concat
//  case "unwanted.txt"                                => MergeStrategy.discard
//  case x =>
//    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
//    oldStrategy(x)
//}

ThisBuild / assemblyMergeStrategy := {
  case PathList("javax", "activation", _*)          => MergeStrategy.first
  case PathList("com", "sun", _*)                   => MergeStrategy.first
  case "META-INF/io.netty.versions.properties"      => MergeStrategy.first
  case "META-INF/mime.types"                        => MergeStrategy.first
  case "META-INF/mailcap.default"                   => MergeStrategy.first
  case "META-INF/mimetypes.default"                 => MergeStrategy.first
  case d if d.endsWith(".jar:module-info.class")    => MergeStrategy.first
  case d if d.endsWith("module-info.class")         => MergeStrategy.first
  case d if d.endsWith("/MatchersBinder.class")     => MergeStrategy.discard
  case d if d.endsWith("/ArgumentsProcessor.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}
