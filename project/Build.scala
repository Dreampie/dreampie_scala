import sbt._, sbt.Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import com.github.play2war.plugin._

object AppBuild extends Build {

  lazy val scalikejdbcVersion = "1.7.4"
  lazy val h2Version = "1.3.174"

  lazy val app = {
    val appName = "dreampie"
    val appVersion = "0.1-SNAPSHOT"
    val appDependencies = Seq(
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion withSources(),
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion withSources(),
      "org.scalikejdbc" %% "scalikejdbc-interpolation" % scalikejdbcVersion withSources(),
      "org.scalikejdbc" %% "scalikejdbc-play-plugin" % scalikejdbcVersion withSources(),
      "org.scalikejdbc" %% "scalikejdbc-play-fixture-plugin" % scalikejdbcVersion withSources(),
      "com.h2database" % "h2" % h2Version withSources(),
      "be.objectify" %% "deadbolt-scala" % "2.2-RC1" withSources(),
      //      "org.elasticsearch" % "elasticsearch" % "1.0.1" withSources(),
      //      "com.clever-age" % "play2-elasticsearch" % "0.8.1" withSources(),
      "com.sksamuel.elastic4s" %% "elastic4s" % "1.0.1.1" withSources(),
      "com.typesafe" %% "play-plugins-util" % "2.2.0" withSources(),
      "com.typesafe" %% "play-plugins-mailer" % "2.2.0" withSources(),
      "org.mindrot" % "jbcrypt" % "0.3m" withSources(),
      "org.slf4j" % "slf4j-simple" % "1.7.6" withSources(),
      "ch.qos.logback" % "logback-classic" % "1.1.1" withSources(),
      //      "org.json4s" %% "json4s-ext" % "3.2.7" withSources(),
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.2" % "optional" exclude("org.scalatest", "scalatest_2.10.0") withSources(),
      "org.json4s" %% "json4s-jackson" % "3.2.7" withSources(),
      "com.github.tototoshi" %% "play-json4s-jackson" % "0.2.0" withSources(),
      "com.github.tototoshi" %% "play-flyway" % "1.0.2" withSources(),
      "net.tanesha.recaptcha4j" % "recaptcha4j" % "0.0.7" withSources(),
      "org.apache.spark" %% "spark-core" % "0.9.0-incubating",
      "org.apache.hadoop" % "hadoop-client" % "2.3.0",
      "org.webjars" %% "webjars-play" % "2.2.1" withSources(),
      "org.webjars" % "jquery" % "1.11.0",
      "org.webjars" % "backbonejs" % "1.1.0",
      "org.webjars" % "underscorejs" % "1.5.2",
      "org.webjars" % "bootstrap" % "3.1.1",
      "org.webjars" % "font-awesome" % "4.0.3",
      //      "org.hibernate"        %  "hibernate-core"                  % "4.2.3.Final"       withSources(),// % "test" withSources(),
      //      "org.eclipse.jetty" % "jetty-webapp" % "9.1.3.v20140225" % "container",
      //      "org.eclipse.jetty" % "jetty-plus" % "9.1.3.v20140225" % "container",
      "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % "test" withSources(),
      "org.specs2" %% "specs2" % "2.1" % "test" withSources()
    )
    play.Project(appName, appVersion, appDependencies, settings = buildSettings ++ sbtassembly.Plugin.assemblySettings
      ++ scalikejdbc.mapper.SbtPlugin.scalikejdbcSettings
      //      ++ com.earldouglas.xsbtwebplugin.WebPlugin.webSettings
      ++ Play2WarPlugin.play2WarSettings
      ++ net.virtualvoid.sbt.graph.Plugin.graphSettings)
      .settings(
        //        publishTo := Some(
        //          "My resolver" at "http://mycompany.com/repo"
        //        ),
        //        credentials += Credentials(
        //          "Repo", "http://mycompany.com/repo", "admin", "admin123"
        //        ),
        scalacOptions ++= Seq("-encoding", "UTF-8"),
        scalaVersion in ThisBuild := "2.10.3",
        conflictWarning := ConflictWarning.disable,
        Play2WarKeys.servletVersion := "3.0",
        resolvers ++= Seq(
          Classpaths.typesafeResolver,
          "Akka Repository" at "http://repo.akka.io/releases/",
          // Change this to point to your local play repository
          "Objectify Play Repository" at "http://schaloner.github.com/releases/",
          "Objectify Play Repository - snapshots" at "http://schaloner.github.com/snapshots/"
          // resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns),
          // resolvers += Resolver.url("Objectify Play Repository - snapshots", url("http://schaloner.github.com/snapshots/"))(Resolver.ivyStylePatterns)
        ),
        testOptions in Test := Nil,
        //        jarName in assembly := "dreampie-front.jar",
        libraryDependencies ~= {
          _ map {
            case m if m.organization == "com.typesafe.play" =>
              m.exclude("commons-logging", "commons-logging")
            //                .exclude("com.typesafe.play", "sbt-link")
            case m if m.organization == "org.slf4j" =>
              m.exclude("org.slf4j", "slf4j-simple")
            case m if m.organization == "javax.servlet" =>
              m.exclude("javax.servlet", "javax.servlet-api")
            case m => m
          }
        },
        mergeStrategy in assembly <<= (mergeStrategy in assembly) {
          (old) => {
            case s: String if s.startsWith("org/mozilla/javascript/") => MergeStrategy.first
            case s: String if s.startsWith("jargs/gnu/") => MergeStrategy.first
            case s: String if s.startsWith("scala/concurrent/stm") => MergeStrategy.first
            case s: String if s.endsWith("ServerWithStop.class") => MergeStrategy.first // There is a scala trait and a Java interface
            case s: String if s.startsWith("Routes") => MergeStrategy.first
            case s: String if s.startsWith("controllers/") => MergeStrategy.first
            case s: String if s.startsWith("conf/") => MergeStrategy.first
            case s: String if s.startsWith("META-INF/") => MergeStrategy.first
            case s: String if s.endsWith("spring.tooling") => MergeStrategy.first
            case s: String if s.endsWith("messages") => MergeStrategy.concat
            case "Global$.class" => MergeStrategy.first
            case "routes" => MergeStrategy.first
            case "deploy.json" => MergeStrategy.first
            case "application.conf" => MergeStrategy.concat
            case "application-logger.xml" => MergeStrategy.first
            case "README" => MergeStrategy.first
            case "CHANGELOG" => MergeStrategy.first
            case x => old(x)
          }
        }
      )
  }

}
