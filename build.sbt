name := "me.dogeshiba.chat"

version := "0.1"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scodec" %% "scodec-bits" % "1.0.6",
  "org.scodec" %% "scodec-core" % "1.7.1"
)

