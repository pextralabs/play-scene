name := "play-scene-actor"

version := "0.1.0"

scalaVersion := "2.12.2"

resolvers += "OSS Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "SCENE Repo" at "https://mymavenrepo.com/repo/BG5Za6vz3CyY7SaQjMOa/"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

libraryDependencies ++= Seq(
  ws,
  guice,
  "com.h2database" % "h2" % "1.4.192",
  "org.drools" % "drools-core" % "7.4.1.Final",
  "org.drools" % "drools-compiler" % "7.4.1.Final",
  "br.ufes.inf.lprm" % "scene-core" % "0.10.8-rc1" exclude("org.slf4j","slf4j-log4j12")
)

//docker setup
packageName in Docker := "pextra/multicast-server"
version in Docker := version.value
maintainer in Docker := "Isaac Pereira"
dockerExposedPorts in Docker := Seq(9000, 9443)
dockerExposedVolumes in Docker := Seq("/opt/docker/logs",
                                      "/opt/docker/conf")

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)


//dockerRepository in Docker := Some("https://hub.docker.com/r/pereirazc/multicast-cloud")