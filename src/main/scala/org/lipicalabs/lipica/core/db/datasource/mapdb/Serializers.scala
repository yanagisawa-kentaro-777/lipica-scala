package org.lipicalabs.lipica.core.db.datasource.mapdb

import java.io.{DataOutput, DataInput}

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.mapdb.Serializer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 19:37
 * YANAGISAWA, Kentaro
 */
object Serializers {

	val ImmutableBytes: Serializer[ImmutableBytes] = new Serializer[ImmutableBytes] {
		override def serialize(out: DataOutput, value: ImmutableBytes) = {
			Serializer.BYTE_ARRAY.serialize(out, value.toByteArray)
		}
		override def deserialize(in: DataInput, available: Int): ImmutableBytes = {
			val bytes = Serializer.BYTE_ARRAY.deserialize(in, available)
			org.lipicalabs.lipica.core.utils.ImmutableBytes(bytes)
		}
	}

}
