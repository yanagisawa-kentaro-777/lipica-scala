package org.lipicalabs.lipica.utils

/**
 * メジャーバージョン、マイナーバージョン、パッチ番号の３要素と、
 * 「SNAPSHOT」、「beta」等のmodifier要素と、
 * ビルド情報を表す要素とから構成されるバージョン情報を表すクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2016/01/28 17:43
 * YANAGISAWA, Kentaro
 */
case class Version(major: Int, minor: Int, patch: Int, modifierOrNone: Option[String], buildLabelOrNone: Option[String]) {
	//
}

object Version {

	def parse(s: String): Either[Throwable, Version] = {
		try {
			val trimmed = s.trim
			val index = trimmed.indexOf('-')
			if (0 <= index) {
				//modifierがある。
				val versionPart = trimmed.substring(0, index)
				val modifierPart = trimmed.substring(index + 1)
				apply(versionPart, modifierPart)
			} else {
				//modifierがない。
				apply(trimmed, null)
			}
		} catch {
			case any: Throwable => Left(any)
		}
	}

	def apply(version: String, modifier: String): Either[Throwable, Version] = {
		apply(version, modifier, null)
	}

	def apply(version: String, modifier: String, build: String): Either[Throwable, Version] = {
		parseDots(version) match {
			case Right(versions) =>
				Right(Version(versions.major, versions.minor, versions.patch, Option(modifier), Option(build)))
			case Left(e) => Left(e)
		}
	}

	private def parseDots(s: String): Either[Throwable, Version] = {
		try {
			val firstIndex = s.indexOf('.')
			val secondIndex = s.indexOf('.', firstIndex + 1)
			val v = Version(s.substring(0, firstIndex).toInt, s.substring(firstIndex + 1, secondIndex).toInt, s.substring(secondIndex + 1).toInt, None, None)
			Right(v)
		} catch {
			case any: Throwable => Left(any)
		}
	}

}
