ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.19"

lazy val root = (project in file("."))
  .settings(
    name := "amitp-scala-project"
  )

// Library dependencies
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "4.8.1" % Test,
  "org.specs2" %% "specs2-junit" % "4.8.1" % Test, // Add this for JUnit support
  "junit" % "junit" % "4.13.2" % Test // JUnit dependency
)

libraryDependencies += "com.wix" %% "http-testkit" % "0.1.23"

libraryDependencies ++= Seq(
  "org.asynchttpclient" % "async-http-client" % "2.11.0",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.10.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.3",
  "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % "2.10.3",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.10.3",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.10.3",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.10.3",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.0.4",
  "org.scalaj" %% "scalaj-http" % "2.4.1"
)

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

libraryDependencies += "com.google.guava" % "guava" % "32.0.1-jre" // or the latest version


// Configure test framework
testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-v")

mainClass in Compile := Some("Main")
