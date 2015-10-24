package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.vm.DataWord

sealed trait MsgType

object MsgType {
	object Call extends MsgType
	object Stateless extends MsgType
	object Post extends MsgType
}


/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class MessageCall private(val msgType: MsgType, val mana: DataWord, val codeAddress: DataWord, val endowment: DataWord,
                          val inDataOffs: DataWord, val inDataSize: DataWord, val outDataOffs: DataWord, val outDataSize: DataWord) {
	//
}

object MessageCall {

	def apply(msgType: MsgType, mana: DataWord, codeAddress: DataWord, endowment: DataWord, inDataOffs: DataWord, inDataSize: DataWord, outDataOffs: DataWord, outDataSize: DataWord): MessageCall = {
		new MessageCall(msgType, mana, codeAddress, endowment, inDataOffs, inDataSize, outDataOffs, outDataSize)
	}

	def apply(msgType: MsgType, mana: DataWord, codeAddress: DataWord, endowment: DataWord, inDataOffs: DataWord, inDataSize: DataWord): MessageCall = {
		new MessageCall(msgType, mana, codeAddress, endowment, inDataOffs, inDataSize, null, null)
	}

}
