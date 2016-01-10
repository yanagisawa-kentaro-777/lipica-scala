version in ThisBuild := "0.5.0.0-SNAPSHOT" // バージョン変更時はここだけ変更すればOK。

organization in ThisBuild := "org.lipicalabs"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "UTF-8", "-target:jvm-1.7")

javacOptions in ThisBuild ++= Seq("-encoding", "UTF-8")

//testOptions in ThisBuild += Tests.Argument(TestFrameworks.ScalaTest, "-u", {val dir = System.getenv("CI_REPORTS"); if(dir == null) "target/reports" else dir} )


//このプロジェクトの依存関係。
//外部ライブラリ。
def commonsCodec = "commons-codec" % "commons-codec" % "1.9"
def commonsIo = "commons-io" % "commons-io" % "2.4"
def lang3 = "org.apache.commons" % "commons-lang3" % "3.2.1"
def slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
def logback = "ch.qos.logback" % "logback-classic" % "1.1.1"
def logbackCore = "ch.qos.logback" % "logback-core" % "1.1.1"

def guava = "com.google.guava" % "guava" % "18.0"
def spongyCastle = "com.madgag.spongycastle" % "core" % "1.53.0.0"

def leveldbIF = "org.iq80.leveldb" % "leveldb" % "0.7"
def leveldbJNI = "org.fusesource.leveldbjni" % "leveldbjni" % "1.8"
def leveldbJNIAll = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

def mapdb = "org.mapdb" % "mapdb" % "2.0-beta10"

def jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % "2.6.3"
def jacksonMapper = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.13"

def nettyAll = "io.netty" % "netty-all" % "4.0.33.Final"

def httpClient = "org.apache.httpcomponents" % "httpclient" % "4.5.1"

def scalatra = "org.scalatra" %% "scalatra" % "2.4.0"
def jettyWebApp = "org.eclipse.jetty" % "jetty-webapp" % "9.3.6.v20151106"

def typesafeConfig = "com.typesafe" % "config" % "1.3.0"
def commonsCli = "commons-cli" % "commons-cli" % "1.3.1"


// テストでしか利用しないライブラリ。
def specs2 = "org.specs2" %% "specs2" % "2.4.2" % "test"


publishMavenStyle in ThisBuild := true

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

// プロジェクト定義。

lazy val root =
	(project in file("."))
		.settings(classDiagramSettings)
		.settings(
			name := "lipica-core",
			libraryDependencies ++= Seq(
				commonsCodec,
				commonsIo,
				typesafeConfig,
				commonsCli,
				lang3,
				leveldbIF,
				leveldbJNI,
				leveldbJNIAll,
				mapdb,
				nettyAll,
				httpClient,
				slf4j,
				logback,
				logbackCore,
				guava,
				spongyCastle,
				jacksonCore,
				jacksonMapper,
				scalatra,
				jettyWebApp,
				specs2
			),
			packSettings,
			// テスト時のみ ./conf配下をクラスパスに追加。
			unmanagedJars in Test ++= Seq(file("./conf"))
		)

