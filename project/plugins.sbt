resolvers ++= Seq(
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe" at "http://repo.typesafe.com/typesafe/repo",
  "Play2war plugins release" at "http://repository-play-war.forge.cloudbees.com/release/",
  "Akka Repository" at "http://repo.akka.io/releases/",
  // Change this to point to your local play repository
  "Objectify Play Repository" at "http://schaloner.github.com/releases/",
  "Objectify Play Repository - snapshots" at "http://schaloner.github.com/snapshots/"
  // resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns),
  // resolvers += Resolver.url("Objectify Play Repository - snapshots", url("http://schaloner.github.com/snapshots/"))(Resolver.ivyStylePatterns)
)

libraryDependencies += "com.h2database" % "h2" % "[1.3,)"

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "[1.7,)") //scalikejdbc-gen [table-name (class-name)]

logLevel := Level.Warn

//addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % Option(System.getProperty("play.version")).getOrElse("2.2.1"))

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2") //assemblyPackageDependency assembly

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4") //dependency-tree what-depends-on <organization> <module> <revision>

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.7.0-SNAPSHOT")

//addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.7.0")

addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.2-beta4") //war