scalaVersion := "2.9.1"

resolvers ++= Seq(
  "twttr.com" at "http://maven.twttr.com",
  "jboss.org" at "http://repository.jboss.org/nexus/content/groups/public/"
)

libraryDependencies <++= (scalaVersion) { scalaVersion =>
  Seq(
    "com.twitter" %% "finagle-core" % "1.9.11",
    "com.twitter" %% "finagle-http" % "1.9.11",
    "com.twitter" %% "finagle-stream" % "1.9.11",
    "com.twitter" %% "util-core" % "1.12.8",
    "org.jboss.netty" % "netty" % "3.2.7.Final",
    "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  )
}

seq(testgenSettings: _*)
