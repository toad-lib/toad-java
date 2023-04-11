import sys.process._
import java.io.File
import sbt.nio.file.FileTreeView

Global / onChangedBuildSource := ReloadOnSourceChanges

val scala3Version = "3.2.2"
val cargoBuild = taskKey[Unit]("cd ./glue; cargo build")
val ejectHeaders = taskKey[Unit]("Generate C headers for FFI")
val fullBuild = taskKey[Unit]("cargoBuild > ejectHeaders")
val glob = settingKey[Map[String, Glob]]("globs")
val path = settingKey[Map[String, String]]("paths")

fork := true

javaOptions += "--enable-preview"

lazy val root = project
  .in(file("."))
  .settings(
    name := "toad",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    glob := Map(
      "java.sources" -> baseDirectory.value.toGlob / "src" / "main" / "java" / ** / "*.java",
      "glue.sources" -> baseDirectory.value.toGlob / "glue" / "src" / ** / "*.rs"
    ),
    Compile / doc / javacOptions ++= Seq(
      "--enable-preview",
      "--release",
      "20"
    ),
    Compile / compile / javacOptions ++= Seq(
      "--enable-preview",
      "--release",
      "20",
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    ),
    path := Map(
      "glue.base" -> (baseDirectory.value / "glue").toString,
      "glue.target" -> (baseDirectory.value / "target" / "glue" / "debug").toString,
      "java.classTarget" -> (baseDirectory.value / "target" / "scala-3.2.2" / "classes").toString
    ),
    ejectHeaders := {
      val files =
        FileTreeView.default.iterator(glob.value("java.sources")).foldLeft("") {
          (s, fd) => s + " " + fd._1.toString
        }
      val cmd =
        Seq(
          "javac",
          "--enable-preview",
          "--release 20",
          "-h " + path.value("glue.target"),
          "-d " + path.value("java.classTarget"),
          files
        )
          .foldLeft("")((s, a) => s + " " + a)

      println(Seq("sh", "-c", cmd) !!)
    },
    cargoBuild := {
      println(Seq("sh", "-c", "cd glue; cargo rustc -- -Awarnings") !!)
      // println(
      //  Seq("sh", "-c", "cd glue; RUST_BACKTRACE=full cargo test --quiet --features e2e") !!
      // ) // very important: test suite validates interfaces
    },
    fullBuild := {
      cargoBuild.value
      ejectHeaders.value
    },
    Compile / compile := (Compile / compile dependsOn fullBuild).value,
    Compile / compile / watchTriggers += glob.value("glue.sources")
  )
