package org.lipicalabs.lipica.core.db.datasource.mapdb

import java.io.{DataOutput, DataInput}

import org.lipicalabs.lipica.core.base.{Block, BlockWrapper}
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

	val BlockWrapper: Serializer[BlockWrapper] = new Serializer[BlockWrapper] {
		override def serialize(out: DataOutput, value: BlockWrapper) = ImmutableBytes.serialize(out, toBytes(value))
		override def deserialize(in: DataInput, available: Int): BlockWrapper = {
			val bytes = ImmutableBytes.deserialize(in, available)
			if (bytes.isEmpty) {
				null
			} else {
				org.lipicalabs.lipica.core.base.BlockWrapper.parse(bytes)
			}
		}
	}

	val Block: Serializer[Block] = new Serializer[Block] {
		override def serialize(out: DataOutput, value: Block) = ImmutableBytes.serialize(out, toBytes(value))

		override def deserialize(in: DataInput, available: Int): Block = {
			val bytes = ImmutableBytes.deserialize(in, available)
			if (bytes.isEmpty) {
				null
			} else {
				org.lipicalabs.lipica.core.base.Block.decode(bytes)
			}
		}
	}

	private def toBytes(block: Block): ImmutableBytes = {
		if (block eq null) {
			org.lipicalabs.lipica.core.utils.ImmutableBytes.empty
		} else {
			block.encode
		}
	}

	private def toBytes(blockWrapper: BlockWrapper): ImmutableBytes = {
		if (blockWrapper eq null) {
			org.lipicalabs.lipica.core.utils.ImmutableBytes.empty
		} else {
			blockWrapper.toBytes
		}
	}

}
