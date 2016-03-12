import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

import sbt.Keys._

scalaJSStage in Global := FastOptStage

skip in packageJSDependencies := false

val app = crossProject.settings(
   isSnapshot := true,
   name := "cataracta",
   organization := "pl.setblack.lsa",
   version := "0.96",
   scalaVersion := "2.11.7",
  unmanagedSourceDirectories in Compile +=
    baseDirectory.value  / "shared" / "main" / "scala",

  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "upickle" % "0.3.8"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework")


).jsSettings(
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "com.github.japgolly.scalajs-react" %%% "core" % "0.10.4",
    "com.github.japgolly.scalajs-react" %%% "extra" % "0.10.4",
    "com.lihaoyi" %%% "scalarx" % "0.2.8"
  ),
  // React itself (react-with-addons.js can be react.js, react.min.js, react-with-addons.min.js)
  jsDependencies ++= Seq(
    "org.webjars.bower" % "react" % "0.14.3" / "react-with-addons.js" commonJSName "React",
    "org.webjars.bower" % "react" % "0.14.3" / "react-dom.js" commonJSName "ReactDOM"),
  skip in packageJSDependencies := false ,// creates app-jsdeps.js with the react JS lib inside
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