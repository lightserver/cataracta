import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

import sbt.Keys._

scalaJSStage in Global := FastOptStage

skip in packageJSDependencies := false

scalaVersion := "2.11.8"

val app = crossProject.settings(
   isSnapshot := true,
   name := "cataracta",
   organization := "pl.setblack.lsa",
   version := "0.97",
   scalaVersion := "2.11.8",
  unmanagedSourceDirectories in Compile +=
    baseDirectory.value  / "shared" / "main" / "scala",

  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "upickle" % "0.3.8",
    "pl.setblack" %%% "cryptotpyrc" % "0.4",
     "biz.enef" %%% "slogging" % "0.4.0"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework")

).jsSettings(
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0"
  ),

  skip in packageJSDependencies := false ,
  persistLauncher in Compile := true
).jvmSettings(
  scalacOptions :=Seq("-Yopt:l:classpath"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.1",
    "com.typesafe.akka" %% "akka-remote" % "2.4.1",
    "org.scalaz" %% "scalaz-core" % "7.1.2",
    "com.typesafe.akka" %% "akka-http-experimental" % "2.0.1",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  )
)

lazy val appJS = app.js.settings(

)

lazy val appJVM = app.jvm.settings(

  resourceDirectory in Compile <<= baseDirectory(_ / "../shared/src/main/resources")

).enablePlugins(JavaAppPackaging)