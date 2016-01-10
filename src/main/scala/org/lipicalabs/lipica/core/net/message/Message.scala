package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * ノード間のあらゆる通信メッセージが実装すべき trait です。
 */
trait Message {
	def isParsed: Boolean
	def toEncodedBytes: ImmutableBytes
	def code: Byte
	def command: Command
	def answerMessage: Option[Class[_ <: ParsedMessage]] = None
}

/**
 * エンコードされた形ではなく、
 * プログラミング言語から扱える形に解析・変換されたメッセージを表す trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:02
 * YANAGISAWA, Kentaro
 */
trait ParsedMessage extends Message {
	override val isParsed: Boolean = true
}

/**
 * メッセージが何の指示であるかを表す列挙型です。
 */
trait Command {
	//
}
