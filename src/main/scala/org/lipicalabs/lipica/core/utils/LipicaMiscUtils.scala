package org.lipicalabs.lipica.core.utils

import scala.reflect.runtime.universe._

/**
 *
 * @since 2015/10/17
 * @author YANAGISAWA, Kentaro
 */
object LipicaMiscUtils {

	def sealedDescendants[Root: TypeTag]: Option[Set[Symbol]] = {
		val symbol = typeOf[Root].typeSymbol
		val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
		if (internal.isSealed)
			Some(internal.sealedDescendants.map(_.asInstanceOf[Symbol]) - symbol)
		else None
	}
}
