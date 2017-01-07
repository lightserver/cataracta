import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

import sbt.Keys._

scalaJSStage in Global := FastOptStage

skip in packageJSDependencies := false


val libName = "cataracta"

scalaVersion in ThisBuild := "2.12.1"
organization in ThisBuild := "pl.setblack.lsa"
name in ThisBuild := libName
version in ThisBuild := "0.98.1"

val app = crossProject.settings(
  isSnapshot := true,
  name := libName,
  unmanagedSourceDirectories in Compile +=
    baseDirectory.value / "shared" / "main" / "scala",

  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "upickle" % "0.4.4",
    "pl.setblack" %%% "cryptotpyrc" % "0.4.1",
    "biz.enef" %%% "slogging" % "0.5.2"
  )
  //,mood.play(compile in Compile, mood.ok, mood.error)

).jsSettings(
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0"
  ),

  skip in packageJSDependencies := false,
  persistLauncher in Compile := true
).jvmSettings(

  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.14",
    "com.typesafe.akka" %% "akka-remote" % "2.4.14",
    "org.scalaz" %% "scalaz-core" % "7.2.8",
    "com.typesafe.akka" %% "akka-http" % "10.0.0",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test"

  )
)

lazy val appJS = app.js.settings(

)

lazy val appJVM = app.jvm.settings(



  resourceDirectory in Compile <<= baseDirectory(_ / "../shared/src/main/resources")

).enablePlugins(JavaAppPackaging)