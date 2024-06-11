name := """play-scala-seed"""
organization := "f"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.14"

libraryDependencies += "com.typesafe.slick" %% "slick" % "3.5.1"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.46.0.0"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.5.1"
libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "f.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "f.binders._"
