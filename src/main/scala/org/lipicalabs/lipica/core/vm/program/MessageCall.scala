package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.vm.DataWord

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
class MessageCall private(val msgType: MessageType, val mana: DataWord, val codeAddress: DataWord, val endowment: DataWord,
                          val inDataOffset: DataWord, val inDataSize: DataWord, val outDataOffset: DataWord, val outDataSize: DataWord) {
	def isStateless: Boolean = {
		this.msgType == MessageType.Stateless
	}
}

object MessageCall {

	def apply(msgType: MessageType, mana: DataWord, codeAddress: DataWord, endowment: DataWord, inDataOffs: DataWord, inDataSize: DataWord, outDataOffs: DataWord, outDataSize: DataWord): MessageCall = {
		new MessageCall(msgType, mana, codeAddress, endowment, inDataOffs, inDataSize, outDataOffs, outDataSize)
	}

	def apply(msgType: MessageType, mana: DataWord, codeAddress: DataWord, endowment: DataWord, inDataOffs: DataWord, inDataSize: DataWord): MessageCall = {
		new MessageCall(msgType, mana, codeAddress, endowment, inDataOffs, inDataSize, null, null)
	}

}
