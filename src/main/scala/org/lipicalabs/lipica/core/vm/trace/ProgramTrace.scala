package org.lipicalabs.lipica.core.vm.trace

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db.ContractDetails
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.{DataWord, OpCode}
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvoke
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/01 11:55
 * YANAGISAWA, Kentaro
 */
class ProgramTrace(programInvoke: ProgramInvoke) {

	import ProgramTrace._

	private val ops = new ArrayBuffer[Op]

	private var _result: String = null
	def result: String = _result
	def result_=(value: String): Unit = {
	  _result = value
	}

	private var _error: String = null
	def error: String = _error
	def error_=(value: String): Unit = {
	  _error = value
	}

	private val initStorage: mutable.Map[String, String] = new mutable.HashMap[String, String]
	private var fullStorage: Boolean = false
	private var storageSize: Int = 0
	private var contractAddress: String = null

	if (SystemProperties.CONFIG.vmTrace && (programInvoke ne null)) {
		this.contractAddress = programInvoke.getOwnerAddress.last20Bytes.toHexString
		val contractDetails = getContractDetails(programInvoke)
		if (contractDetails eq null) {
			this.storageSize = 0
			this.fullStorage = true
		} else {
			this.storageSize = contractDetails.getStorageSize
			if (this.storageSize <= SystemProperties.CONFIG.vmTraceInitStorageLimit) {
				this.fullStorage = true
				val address = programInvoke.getOwnerAddress.last20Bytes.toHexString
				contractDetails.getStorage.foreach {entry => {
					val (key, value) = entry
					if ((key eq null) || (value eq null)) {
						logger.info("Null storage key/value: address[%s]".format(address))
					} else {
						this.initStorage.put(key.toHexString, value.toHexString)
					}
				}}
				if (this.initStorage.nonEmpty) {
					logger.info("%,d entries loaded to transaction's initStorage.".format(this.initStorage.size))
				}
			}
		}
	}

	def result(v: ImmutableBytes): ProgramTrace = {
		result = v.toHexString
		this
	}

	def error(e: Exception): ProgramTrace = {
		val s = "%s: %s".format(e.getClass, e.getMessage)
		error = s
		this
	}

	def addOp(code: Byte, pc: Int, deep: Int, mana: DataWord, actions: OpActions): Op = {
		val op = new Op
		op.actions = actions
		op.code = OpCode.code(code).orNull
		op.deep = deep
		op.mana = mana.value
		op.pc = pc
		this.ops.append(op)
		op
	}

	def mergeToThis(another: ProgramTrace): Unit = {
		this.ops ++= another.ops
	}

	private def getContractDetails(programInvoke: ProgramInvoke): ContractDetails = {
		val repository = programInvoke.getRepository
		//TODO repository が RepositoryTrack だったら、本体を引き寄せること。

		val address = programInvoke.getOwnerAddress.last20Bytes
		repository.getContractDetails(address)
	}

	//TODO json化

}

object ProgramTrace {

	private val logger = LoggerFactory.getLogger("VM")

	class Op {
		private var _code: OpCode = null
		def code: OpCode = _code
		def code_=(value: OpCode): Unit = {
		  _code = value
		}

		private var _deep: Int = 0
		def deep: Int = _deep
		def deep_=(value: Int): Unit = {
		  _deep = value
		}

		private var _pc: Int = 0
		def pc: Int = _pc
		def pc_=(value: Int): Unit = {
		  _pc = value
		}

		private var _mana: BigInt = null
		def mana: BigInt = _mana
		def mana_=(value: BigInt): Unit = {
		  _mana = value
		}

		private var _actions: OpActions = null
		def actions: OpActions = _actions
		def actions_=(value: OpActions): Unit = {
		  _actions = value
		}

	}

}