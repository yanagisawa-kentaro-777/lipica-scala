package org.lipicalabs.lipica.core.validator

import org.slf4j.Logger

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:01
 * YANAGISAWA, Kentaro
 */
abstract class AbstractValidationRule extends ValidationRule {

	protected val errors = new ArrayBuffer[String]

	override def getErrors: Iterable[String] = this.errors.toIterable

	def logErrors(logger: Logger): Unit = {
		for (each <- this.errors) {
			logger.warn("<Validator> [%s] %s".format(getEntityClass, each))
		}
	}

	def getEntityClass: Class[_]

}
