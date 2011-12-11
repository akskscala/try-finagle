resolvers ++= Seq(
  "seratch.github.com" at "http://seratch.github.com/mvn-repo/releases",
  "mpeltonen.github.com" at "http://mpeltonen.github.com/maven/"
)

addSbtPlugin("com.github.seratch" %% "testgen-sbt" % "0.2")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "0.11.0")


