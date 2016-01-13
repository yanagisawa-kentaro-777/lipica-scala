package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.Logger

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:01
 * YANAGISAWA, Kentaro
 */
trait AbstractValidationRule extends ValidationRule {

	protected val errors = new ArrayBuffer[String]

	override def getErrors: Iterable[String] = this.errors.toIterable

	def logErrors(logger: Logger): Unit = {
		for (each <- this.errors) {
			ErrorLogger.logger.warn("<Validator> [%s] %s".format(getEntityClass.getSimpleName, each))
			logger.warn("<Validator> [%s] %s".format(getEntityClass.getSimpleName, each))
		}
	}

	def getEntityClass: Class[_]

}
