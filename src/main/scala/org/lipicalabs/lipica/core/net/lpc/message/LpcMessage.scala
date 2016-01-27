package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.Message

/**
 * ブロックやトランザクションに関する情報の授受を実行するメッセージの
 * 実装基底クラスです。
 *
 * Created by IntelliJ IDEA.
 * @since 2015/12/08 19:57
 * @author YANAGISAWA, Kentaro
 */
abstract class LpcMessage extends Message {
	override def command = LpcMessageCode.fromByte(this.code)
}
