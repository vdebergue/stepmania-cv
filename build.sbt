name := "stepmania-cv"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

// to use ffmpeg modules
javaCppPresetLibs ++= Seq(
  "ffmpeg" -> "2.8.1"
)

// enablePlugins(UniversalPlugin)

enablePlugins(JavaAppPackaging)
