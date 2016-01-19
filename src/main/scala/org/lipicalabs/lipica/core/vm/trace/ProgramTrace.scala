package org.lipicalabs.lipica.core.vm.trace

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.datastore.{RepositoryTrackLike, Repository}
import org.lipicalabs.lipica.core.kernel.ContractDetails
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.{DataWord, OpCode}
import org.lipicalabs.lipica.core.vm.program.context.ProgramContext
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/01 11:55
 * YANAGISAWA, Kentaro
 */
class ProgramTrace(private val programContext: ProgramContext) {

	import ProgramTrace._

	private val ops = new ArrayBuffer[Op]

	private val resultRef: AtomicReference[String] = new AtomicReference[String](null)
	def result: String = resultRef.get
	def result_=(value: String): Unit = resultRef.set(value)

	private val errorRef: AtomicReference[String] = new AtomicReference[String](null)
	def error: String = errorRef.get
	def error_=(value: String): Unit = errorRef.set(value)

	private val initStorage: mutable.Map[String, String] = new mutable.HashMap[String, String]
	private var fullStorage: Boolean = false
	private var storageSize: Int = 0
	private var contractAddress: String = null

	if (NodeProperties.CONFIG.vmTrace && (programContext ne null)) {
		this.contractAddress = programContext.ownerAddress.last20Bytes.toHexString
		getContractDetails(programContext) match {
			case Some(contractDetails) =>
				this.storageSize = contractDetails.storageSize
				if (this.storageSize <= NodeProperties.CONFIG.vmTraceInitStorageLimit) {
					this.fullStorage = true
					val address = programContext.ownerAddress.last20Bytes.toHexString
					contractDetails.storageContent.foreach { entry => {
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
			case None =>
				this.storageSize = 0
				this.fullStorage = true
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

	private def getContractDetails(programContext: ProgramContext): Option[ContractDetails] = {
		val repository = programContext.repository match {
			case track: RepositoryTrackLike => track.originalRepository
			case repos: Repository => repos
		}
		val address = programContext.ownerAddress.last20Bytes
		repository.getContractDetails(address)
	}

	//TODO json化

}

object ProgramTrace {

	private val logger = LoggerFactory.getLogger("vm")

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