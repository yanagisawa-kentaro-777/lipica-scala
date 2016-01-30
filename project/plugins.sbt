logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

// Plugin for scoverage:
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.4")

// Plugin for publishing scoverage results to coveralls:
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.0.0")

// Plugin for building separate JARS.
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.8")

addSbtPlugin("com.github.xuwei-k" % "sbt-class-diagram" % "0.1.4")

// Plugin for building a fat JAR.
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
