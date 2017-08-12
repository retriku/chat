import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import play.core.PlayVersion.{current => playVersion}

import scalariform.formatter.preferences.{AlignParameters, AlignSingleLineCaseStatements, RewriteArrowSymbols, SpacesAroundMultiImports}

name := "chat"
organization := "lv"
version := "1.0"

scalaVersion := "2.12.3"

scalacOptions ++= Seq("-feature", "-deprecation")
scalacOptions in(Compile, doc) := Nil

val AkkaVersion = "2.5.4"

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "1.2.0" withSources() withJavadoc(),
  "org.sangria-graphql" %% "sangria-spray-json" % "1.0.0" withSources() withJavadoc(),
  "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.0" withSources() withJavadoc(),

  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",

  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  // Production dependencies
  "com.typesafe.play" %% "play" % playVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-remote" % AkkaVersion withSources() withJavadoc(),
  "com.softwaremill.macwire" %% "macros" % "2.3.0" % Provided withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-cluster" % AkkaVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion withSources() withJavadoc(),
  "com.lightbend.play" %% "play-socket-io" % "1.0.0-beta-2" withSources() withJavadoc(),

  // Test dependencies for running a Play server
  "com.typesafe.play" %% "play-akka-http-server" % playVersion % Test withSources() withJavadoc(),
  "com.typesafe.play" %% "play-logback" % playVersion % Test withSources() withJavadoc(),
  // Test dependencies for Scala/Java dependency injection
  "com.typesafe.play" %% "play-guice" % playVersion % Test withSources() withJavadoc(),
  "com.softwaremill.macwire" %% "macros" % "2.3.0" % Test withSources() withJavadoc(),
  // Test framework dependencies
  "org.scalatest" %% "scalatest" % "3.0.1" % Test withSources() withJavadoc(),
  "com.novocode" % "junit-interface" % "0.11" % Test withSources() withJavadoc()
)

fork in Test := true

scalariformPreferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, false)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(SpacesAroundMultiImports, true)

enablePlugins(PlayScala)

