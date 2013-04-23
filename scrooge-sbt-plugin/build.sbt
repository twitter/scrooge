sbtPlugin := true

name := "sbt-scrooge-plugin"

organization := "com.twitter"

version := "3.1.1"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
		    "com.google.collections" % "google-collections" % "0.8",
		    "org.codehaus.plexus"    % "plexus-utils"       % "1.5.4",
		    "org.slf4j"              % "slf4j-api"          % "1.6.1",
		    "org.slf4j"              % "slf4j-simple"       % "1.6.1",
		    "com.twitter"            % "scrooge-generator"  % "3.1.1")
