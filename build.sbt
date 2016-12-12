import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

val akkaVer = "2.4.0"
val logbackVer = "1.1.3"
val scalaVer = "2.11.7"
val scalaParsersVer= "1.0.4"
val scalaTestVer = "2.2.4"

lazy val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

lazy val commonDependencies = Seq(
  "com.typesafe.akka"         %% "akka-actor"                          % akkaVer,
  "com.typesafe.akka"         %% "akka-cluster"                        % akkaVer,
  "com.typesafe.akka"         %% "akka-cluster-tools"                  % akkaVer,
  "com.typesafe.akka"         %% "akka-cluster-sharding"               % akkaVer,
  "com.typesafe.akka"         %% "akka-distributed-data-experimental"  % akkaVer,
  "com.typesafe.akka"         %% "akka-persistence"                    % akkaVer,
  "com.typesafe.akka"         %% "akka-slf4j"                          % akkaVer,
  "ch.qos.logback"            %  "logback-classic"                     % logbackVer,
  "org.scala-lang.modules"    %% "scala-parser-combinators"            % scalaParsersVer,
  "org.iq80.leveldb"          % "leveldb"                              % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all"                       % "1.8",
  "com.typesafe.akka"         %% "akka-testkit"                        % akkaVer            % "test",
  "org.scalatest"             %% "scalatest"                           % scalaTestVer       % "test",
  "com.typesafe.akka"         %% "akka-multi-node-testkit"             % akkaVer            % "test",
  "commons-io"                %  "commons-io"                          % "2.4"              % "test",
  // avoids warnings about conflicting transitive dependencies
  "org.scala-lang"            % "scala-reflect"                        % scalaVer,
  "org.scala-lang.modules"    %% "scala-xml"                           % "1.0.4"

)

lazy val commonResolvers = Seq(
  "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven"
)

lazy val fttas = project in file(".")
name := "akka-collect"
organization := "com.typesafe.training"
version := "1.0.0"
scalaVersion := scalaVer
scalacOptions ++= compileOptions
unmanagedSourceDirectories in Compile := List((scalaSource in Compile).value)
unmanagedSourceDirectories in Test := List((scalaSource in Test).value)
EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
EclipseKeys.eclipseOutput := Some(".target")
EclipseKeys.withSource := true
parallelExecution in Test := false
logBuffered in Test := false
parallelExecution in ThisBuild := false
libraryDependencies ++= commonDependencies
resolvers ++= commonResolvers

SbtMultiJvm.multiJvmSettings
compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test)
// make sure that MultiJvm tests are executed by the default test target,
// and combine the results from ordinary test and multi-jvm tests
executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
  case (testResults, multiNodeResults)  =>
    val overall =
      if (testResults.overall.id < multiNodeResults.overall.id)
        multiNodeResults.overall
      else
        testResults.overall
    Tests.Output(overall,
      testResults.events ++ multiNodeResults.events,
      testResults.summaries ++ multiNodeResults.summaries)
}
configs(MultiJvm)

addCommandAlias("ge", "runMain com.typesafe.training.akkacollect.GameEngineApp -Dakka.remote.netty.tcp.port=2551 -Dakka.cluster.roles.0=game-engine")
addCommandAlias("pr", "runMain com.typesafe.training.akkacollect.PlayerRegistryApp -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.roles.0=player-registry")
addCommandAlias("sr", "runMain com.typesafe.training.akkacollect.ScoresRepositoryApp -Dakka.remote.netty.tcp.port=2553 -Dakka.cluster.roles.0=scores-repository")
addCommandAlias("ge2", "runMain com.typesafe.training.akkacollect.GameEngineApp -Dakka.remote.netty.tcp.port=2554 -Dakka.cluster.roles.0=game-engine")
addCommandAlias("pr2", "runMain com.typesafe.training.akkacollect.PlayerRegistryApp -Dakka.remote.netty.tcp.port=2555 -Dakka.cluster.roles.0=player-registry")
addCommandAlias("sr2", "runMain com.typesafe.training.akkacollect.ScoresRepositoryApp -Dakka.remote.netty.tcp.port=2556 -Dakka.cluster.roles.0=scores-repository")
addCommandAlias("sj", "runMain com.typesafe.training.akkacollect.SharedJournalApp -Dakka.remote.netty.tcp.port=2550 -Dakka.cluster.roles.0=shared-journal")
