scalaVersion := "2.9.1"

resolvers ++= Seq(
  "twttr.com" at "http://maven.twttr.com",
  "jboss.org" at "http://repository.jboss.org/nexus/content/groups/public/"
)

libraryDependencies <++= (scalaVersion) { scalaVersion =>
  Seq(
    "com.twitter" %% "finagle-core" % "4.0.2",
    "com.twitter" %% "finagle-http" % "4.0.2",
    "com.twitter" %% "finagle-stream" % "4.0.2",
    "com.twitter" %% "util-core" % "4.0.1",
    "io.netty" % "netty" % "3.4.5.Final",
    "com.github.jsuereth.scala-arm" %% "scala-arm" % "1.1" % "test",
    "org.scalatest" %% "scalatest" % "1.7.1" % "test"
  )
}

seq(testgenSettings: _*)
