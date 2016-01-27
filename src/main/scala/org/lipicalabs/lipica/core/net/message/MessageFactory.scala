package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * メッセージの生成者が実装すべき trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:09
 * YANAGISAWA, Kentaro
 */
trait MessageFactory {
	def create(code: Byte, encodedBytes: ImmutableBytes): Option[Message]
}
