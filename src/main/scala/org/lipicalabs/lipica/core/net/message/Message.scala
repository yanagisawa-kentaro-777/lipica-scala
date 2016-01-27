package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * 確立された伝送路上での通信メッセージが実装すべき trait です。
 */
trait Message {

	/**
	 * このメッセージの種別を表現する列挙型の値を返します。
	 */
	def command: Command

	/**
	 * このメッセージの種別を表現する１バイトを返します。
	 */
	def code: Byte

	/**
	 * このインスタンスの内容をバイト列に変換して返します。
	 */
	def toEncodedBytes: ImmutableBytes

	/**
	 * このメッセージが返信を期待するものである場合に、
	 * その返信の型を返します。
	 */
	def answerMessage: Option[Class[_ <: Message]] = None

}

/**
 * メッセージが何の指示であるかを表す列挙型です。
 */
trait Command {
	//
}
