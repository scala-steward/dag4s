val scalaSettings = Seq(
  scalaVersion := "2.13.1",
  scalacOptions ++= compilerOptions
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint",
  "-language:_",
  "-Ypartial-unification" /*,
  "-Xfatal-warnings"*/
)

name := "dag4s"
organization := "dmarticus"
version := "0.0.1"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.1.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1"

// Create a test Scala style task to run with tests
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := scalastyle.in(Test).toTask("").value
(test in Test) := ((test in Test) dependsOn testScalastyle).value
(scalastyleConfig in Test) := baseDirectory.value / "scalastyle-test-config.xml"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := scalastyle.in(Compile).toTask("").value
(test in Test) := ((test in Test) dependsOn compileScalastyle).value

scalastyleFailOnError := true
scalastyleFailOnWarning := true
(scalastyleFailOnError in Test) := true
(scalastyleFailOnWarning in Test) := true
