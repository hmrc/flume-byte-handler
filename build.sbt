/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gatling.sbt.GatlingPlugin
import sbt._
import sbt.Keys.{parallelExecution, _}
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName = "flume-byte-handler"
val Benchmark = config("bench") extend Test

val compileDeps = Seq(
  "org.apache.flume" % "flume-ng-core" % "1.7.0" % "provided,bench",
  "org.apache.flume" % "flume-ng-sdk" % "1.7.0" % "provided,bench",
  "org.slf4j" % "slf4j-api" % "1.7.25" % Provided
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "org.pegdown" % "pegdown" % "1.5.0" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)

val itDeps = Seq(
  "com.storm-enroute" %% "scalameter" % "0.8.2" % Benchmark
)

val gatlingDeps = Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.5" % IntegrationTest,
  "io.gatling" % "gatling-test-framework" % "2.2.5" % IntegrationTest
)

val commonSettings = Seq(
  scalaVersion := "2.11.7",
  resolvers := Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  crossPaths := false,
  version := "0.1-SNAPSHOT"
)// ++ SbtAutoBuildPlugin.projectSettings

lazy val `flume-byte-handler` = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .disablePlugins(AssemblyPlugin)
  .aggregate(handler, gatling)
  .settings(
    commonSettings
  )

lazy val handler = (project in file("handler"))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, AssemblyPlugin)
  .configs(Benchmark)
  .settings(
    commonSettings,
    name := appName,
    libraryDependencies ++= compileDeps ++ testDeps ++ itDeps,
    assemblyJarName in assembly := s"${name.value}-with-scala-${version.value}.jar",
    test in assembly := {},

    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    parallelExecution in Benchmark := false,
    testOptions in Benchmark := Seq(),
    inConfig(Benchmark)(Defaults.testSettings)
  )

lazy val gatling = (project in file("gatling"))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, GatlingPlugin)
  .disablePlugins(AssemblyPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= gatlingDeps,
    name := s"$appName-gatling"
  )
