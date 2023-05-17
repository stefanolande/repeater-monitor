val scala3Version = "3.2.2"

val http4sVersion          = "0.23.19"
val logbackVersion         = "1.4.6"
val munitCatsEffectVersion = "2.0.0-M3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "repeater-monitor",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "org.typelevel" %% "munit-cats-effect" % munitCatsEffectVersion % "test"
    )
  )
