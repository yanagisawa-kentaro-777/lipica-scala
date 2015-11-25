package org.lipicalabs.lipica.core.db.datasource.mapdb

import java.io.{DataOutput, DataInput}

import org.lipicalabs.lipica.core.base.BlockWrapper
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.mapdb.Serializer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 19:37
 * YANAGISAWA, Kentaro
 */
object Serializers {

	val BlockWrapper: Serializer[BlockWrapper] = new Serializer[BlockWrapper] {

		override def serialize(dataOutput: DataOutput, a: BlockWrapper) = ???

		override def deserialize(dataInput: DataInput, i: Int) = ???
	}

	val ImmutableBytes: Serializer[ImmutableBytes] = new Serializer[ImmutableBytes] {

		override def serialize(dataOutput: DataOutput, a: ImmutableBytes) = ???

		override def deserialize(dataInput: DataInput, i: Int) = ???
	}

}
