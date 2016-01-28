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
case class Version(major: Int, minor: Int, patch: Int, modifierOrNone: Option[String], buildLabelOrNone: Option[String]) extends Comparable[Version] {

	override def compareTo(another: Version): Int = {
		if (this.major < another.major) {
			-1
		} else if (this.major > another.major) {
			1
		} else if (this.minor < another.minor) {
			-1
		} else if (this.minor > another.minor) {
			1
		} else if (this.patch < another.patch) {
			-1
		} else if (this.patch > another.patch) {
			1
		} else {
			this.buildLabelOrNone.getOrElse("").compareTo(another.buildLabelOrNone.getOrElse(""))
		}
	}

	override def hashCode: Int = this.toCanonicalString.hashCode

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[Version]
			(this.major == another.major) && (this.minor == another.minor) && (this.patch == another.patch) &&
				(this.modifierOrNone == another.modifierOrNone) && (this.buildLabelOrNone == another.buildLabelOrNone)
		} catch {
			case any: Throwable => false
		}
	}

	def toCanonicalString: String = {
		val version = "%d.%d.%d".format(this.major, this.minor, this.patch)
		val modifier = this.modifierOrNone.map(s => "-" + s).getOrElse("")
		val build = this.buildLabelOrNone.map(s => " BUILD:" + s).getOrElse("")
		version + modifier + build
	}

	override def toString: String = toCanonicalString

}

object Version {

	val zero = Version(0, 0, 0, None, None)

	def parse(s: String): Either[Throwable, Version] = {
		try {
			val trimmed = s.trim
			val index = trimmed.indexOf('-')
			if (0 <= index) {
				//modifierがある。
				val versionPart = trimmed.substring(0, index)
				val modifierPart = trimmed.substring(index + 1)
				parse(versionPart, modifierPart)
			} else {
				//modifierがない。
				parse(trimmed, null)
			}
		} catch {
			case any: Throwable => Left(any)
		}
	}

	def parse(version: String, modifier: String): Either[Throwable, Version] = {
		parse(version, modifier, null)
	}

	def parse(version: String, modifier: String, build: String): Either[Throwable, Version] = {
		parseDots(version) match {
			case Right(versions) =>
				Right(Version(versions.major, versions.minor, versions.patch, Option(modifier), Option(build)))
			case Left(e) => Left(e)
		}
	}

	def parse(version: String, modifier: Option[String], build: Option[String]): Either[Throwable, Version] = {
		parseDots(version) match {
			case Right(versions) =>
				Right(Version(versions.major, versions.minor, versions.patch, modifier, build))
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
