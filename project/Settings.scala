/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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
trait Settings {

  import org.flywaydb.sbt.FlywayPlugin._
  import org.flywaydb.sbt.FlywayPlugin.autoImport._
  import CustomSettings._
  import sbt.Keys._
  import sbt._

  lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % Versions.kindProjector),
    scalaVersion := Versions.scala,
    organization := "com.fortysevendeg",
    organizationName := "47 Degrees",
    organizationHomepage := Some(new URL("http://47deg.com")),
    version := Versions.buildVersion,
    conflictWarning := ConflictWarning.disable,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:higherKinds", "-language:implicitConversions", "-Ywarn-unused-import", "-Xfatal-warnings"),
    javaOptions in Test ++= Seq("-XX:MaxPermSize=128m", "-Xms512m", "-Xmx512m"),
    sbt.Keys.fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in(Test, packageSrc) := true,
    logLevel := Level.Info,
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.defaultLocal,
      Classpaths.typesafeReleases,
      Resolver.bintrayRepo("scalaz", "releases"),
      DefaultMavenRepository,
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Flyway" at "https://flywaydb.org/repo",
      "RoundEights" at "http://maven.spikemark.net/roundeights"
    ),
    doc in Compile <<= target.map(_ / "none")
  ) ++ Scalariform9C.settings

  lazy val processesSettings = projectSettings ++ Seq(
    apiResourcesFolder := apiResourcesFolderDef.value,
    unmanagedClasspath in Test += apiResourcesFolder.value
  )

  lazy val flywayTestSettings9C = {
    flywayBaseSettings(Test) ++ Seq(
      flywayDriver := "org.postgresql.Driver",
      flywayUrl := "jdbc:postgresql://localhost/ninecards_test",
      flywayUser := "ninecards_tester",
      flywayPassword := "",
      flywayLocations := Seq("filesystem:" + apiResourcesFolder.value.getPath + "/db/migration")
    )
  }

  lazy val serviceSettings = projectSettings ++ Seq(
    apiResourcesFolder := apiResourcesFolderDef.value,
    unmanagedClasspath in Test += apiResourcesFolder.value,
    parallelExecution in Test := false,
    test <<= test in Test dependsOn flywayMigrate,
    testOnly <<= testOnly in Test dependsOn flywayMigrate,
    testQuick <<= testQuick in Test dependsOn flywayMigrate
  ) ++ flywayTestSettings9C

  lazy val googleplaySettings = {
    val protoBufSettings = {
      import sbtprotobuf.{ProtobufPlugin => PB}
      PB.protobufSettings ++ Seq(
        PB.runProtoc in PB.protobufConfig := { args =>
          com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray)
        }
      )
    }
    projectSettings ++ protoBufSettings
  }

}