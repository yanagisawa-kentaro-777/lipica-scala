package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.vm.VMWord

sealed trait MessageType

object MessageType {
	object Call extends MessageType
	object Stateless extends MessageType
	object Post extends MessageType
}


/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class MessageCall private(val msgType: MessageType, val mana: VMWord, val codeAddress: VMWord, val endowment: VMWord,
                          val inDataOffset: VMWord, val inDataSize: VMWord, val outDataOffset: VMWord, val outDataSize: VMWord) {
	def isStateless: Boolean = {
		this.msgType == MessageType.Stateless
	}
}

object MessageCall {

	def apply(msgType: MessageType, mana: VMWord, codeAddress: VMWord, endowment: VMWord, inDataOffs: VMWord, inDataSize: VMWord, outDataOffs: VMWord, outDataSize: VMWord): MessageCall = {
		new MessageCall(msgType, mana, codeAddress, endowment, inDataOffs, inDataSize, outDataOffs, outDataSize)
	}

	def apply(msgType: MessageType, mana: VMWord, codeAddress: VMWord, endowment: VMWord, inDataOffs: VMWord, inDataSize: VMWord): MessageCall = {
		new MessageCall(msgType, mana, codeAddress, endowment, inDataOffs, inDataSize, null, null)
	}

}
