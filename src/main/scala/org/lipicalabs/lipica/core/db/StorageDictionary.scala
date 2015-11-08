package org.lipicalabs.lipica.core.db

/**
 * Created by IntelliJ IDEA.
 * 2015/11/04 20:01
 * YANAGISAWA, Kentaro
 */
object StorageDictionary {

	sealed trait Type
	object Type {
		case object Root extends Type
		case object StorageIndex extends Type
		case object Offset extends Type
		case object ArrayIndex extends Type
		case object Map extends Type
	}

	class PathElement {
		//
	}

}
