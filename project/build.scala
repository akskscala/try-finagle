import sbt._
import Keys._

object TryFinagleBuild extends Build {

  lazy val jdbcPattern = Project("try-finagle", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    sbtPlugin := false,
    organization := "akskscala",
    name := "try-finagle",
    version := "0.1",
    publishTo <<= (version) { version: String =>
      Some(
        Resolver.file("GitHub Pages", Path.userHome / "github" / "seratch.github.com" / "mvn-repo" / {
          if (version.trim.endsWith("SNAPSHOT")) "snapshots" else "releases" 
        })
      )
    },
    publishMavenStyle := true,
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

}

