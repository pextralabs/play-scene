name := "play-scene-actor"

version := "0.1.0"

scalaVersion := "2.12.2"

resolvers += "OSS Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "SCENE Repo" at "https://mymavenrepo.com/repo/BG5Za6vz3CyY7SaQjMOa/"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  ws,
  guice,
  evolutions,
  jdbc,
  javaJpa,
  "org.hibernate" % "hibernate-core" % "5.2.5.Final",
  "com.h2database" % "h2" % "1.4.192",
  "org.drools" % "drools-core" % "7.4.1.Final",
  "org.drools" % "drools-compiler" % "7.4.1.Final",
  "br.ufes.inf.lprm" % "scene-core" % "0.10.8-rc1" exclude("org.slf4j","slf4j-log4j12"),
  // Testing libraries for dealing with CompletionStage...
  "org.assertj" % "assertj-core" % "3.8.0" % Test,
  "org.awaitility" % "awaitility" % "3.0.0" % Test
)



javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)