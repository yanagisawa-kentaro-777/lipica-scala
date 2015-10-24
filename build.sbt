version in ThisBuild := "0.5.0.0-SNAPSHOT" // バージョン変更時はここだけ変更すればOK。

organization in ThisBuild := "org.lipicalabs"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "UTF-8", "-target:jvm-1.7")

// このプロジェクトの依存関係。
// 外部ライブラリ。
def commonsCodec = "commons-codec"          % "commons-codec"             % "1.9"
def commonsIo    = "commons-io"             % "commons-io"                % "2.4"
def lang3        = "org.apache.commons"     % "commons-lang3"             % "3.2.1"
def slf4j        = "org.slf4j"              % "slf4j-api"                 % "1.7.5"
def logback      = "ch.qos.logback"         % "logback-classic"           % "1.1.1"
def logbackCore  = "ch.qos.logback"         % "logback-core"              % "1.1.1"
def metrics      = "com.codahale.metrics"   % "metrics-core"              % "3.0.2"
def parserComb   = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2"

def guava = "com.google.guava" % "guava" % "18.0"
def spongyCastle = "com.madgag.spongycastle" % "core" % "1.53.0.0"

// テストでしか利用しないライブラリ。
def specs2 = "org.specs2" %% "specs2" % "2.4.2" % "test"

def scalaReflect = "org.scala-lang" % "scala-reflect" % "2.11.6"
def scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"


def commonsCli = "commons-cli" % "commons-cli" % "1.3.1"


publishMavenStyle in ThisBuild := true

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)



// プロジェクト定義。

lazy val lipicaCore =
	(project in file("lipica-core"))
		.settings(classDiagramSettings)
		.settings(
			name := "lipica-core",
			libraryDependencies ++= Seq(
				commonsCodec,
				commonsIo,
				lang3,
				slf4j,
				logback,
				logbackCore,
				metrics,
				guava,
				spongyCastle,
				parserComb,
				scalaReflect,
				specs2
			),
			packSettings,
			// テスト時のみ ./conf配下をクラスパスに追加。
			unmanagedJars in Test ++= Seq(file("./conf"))
		)




lazy val all = lipicaCore
