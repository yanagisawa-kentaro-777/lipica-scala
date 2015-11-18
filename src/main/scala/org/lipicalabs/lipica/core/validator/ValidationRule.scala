package org.lipicalabs.lipica.core.validator

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:01
 * YANAGISAWA, Kentaro
 */
trait ValidationRule {

	def getErrors: Iterable[String]

}
