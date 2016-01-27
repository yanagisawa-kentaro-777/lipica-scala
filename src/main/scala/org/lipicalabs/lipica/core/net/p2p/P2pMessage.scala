package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.Message

/**
 * セッションの維持管理に関するメッセージの実装基底クラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/04 20:50
 * YANAGISAWA, Kentaro
 */
abstract class P2PMessage extends Message {
	override def command = P2PMessageCode.fromByte(this.code)
}
