logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.3.3")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.8")

addSbtPlugin("com.github.xuwei-k" % "sbt-class-diagram" % "0.1.4")
