import play.core.PlayVersion.{current => playVersion}

name := "chat"
organization := "lv"
version := "1.0"

scalaVersion := "2.12.3"

scalacOptions ++= Seq("-feature", "-Xfatal-warnings")
scalacOptions in(Compile, doc) := Nil

val AkkaVersion = "2.5.3"

libraryDependencies ++= Seq(
  // Production dependencies
  "com.typesafe.play" %% "play" % playVersion,
  "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
  "com.softwaremill.macwire" %% "macros" % "2.3.0" % Provided,
  "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion,
  "com.lightbend.play" %% "play-socket-io" % "1.0.0-beta-2",

  // Test dependencies for running a Play server
  "com.typesafe.play" %% "play-akka-http-server" % playVersion % Test,
  "com.typesafe.play" %% "play-logback" % playVersion % Test,

  // Test dependencies for Scala/Java dependency injection
  "com.typesafe.play" %% "play-guice" % playVersion % Test,
  "com.softwaremill.macwire" %% "macros" % "2.3.0" % Test,

  // Test framework dependencies
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test
)

fork in Test := true

def formattingPreferences = {
  import scalariform.formatter.preferences._
  FormattingPreferences()
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(SpacesAroundMultiImports, true)
}

enablePlugins(PlayScala)

