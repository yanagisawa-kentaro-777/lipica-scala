package org.lipicalabs.lipica.core.kernel

import java.nio.charset.StandardCharsets
import java.util

import org.codehaus.jackson.map.ObjectMapper
import org.lipicalabs.lipica.core.kernel.CallTransaction.Type.{StringType, IntType}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/24 17:01
 * YANAGISAWA, Kentaro
 */
object CallTransaction {

	def createRawTransaction(nonce: Long, manaPrice: Long, manaLimit: Long, toAddress: String, value: Long, data: ImmutableBytes): TransactionLike = {
		Transaction(
			nonce = ByteUtils.toByteArrayWithNoLeadingZeros(nonce),
			manaPrice = ByteUtils.toByteArrayWithNoLeadingZeros(manaPrice),
			manaLimit = ByteUtils.toByteArrayWithNoLeadingZeros(manaLimit),
			receiveAddress = Address160.parseHexString(toAddress),
			value = ByteUtils.toByteArrayWithNoLeadingZeros(value),
			data = data
		)
	}

	def createCallTransaction(nonce: Long, manaPrice: Long, manaLimit: Long, toAddress: String, value: Long, callFunc: Function, funcArgs: Any*): TransactionLike = {
		val callData = callFunc.encode(funcArgs: _*)
		createRawTransaction(nonce, manaPrice, manaLimit, toAddress, value, callData)
	}

	sealed abstract class Type(val name: String) {
		def canonicalName: String = this.name
		def encode(value: Any): ImmutableBytes
		def decode(encoded: ImmutableBytes): Any
		def fixedSize: Int = 32
		override def toString: String = this.name
	}

	object Type {
		def getType(aTypeName: String): Type = {
			val typeName = aTypeName.trim.toLowerCase
			if (typeName.contains("[")) return ArrayType.getType(typeName)
			if ("bool" == typeName) return new BoolType
			if (typeName.startsWith("int") || typeName.startsWith("uint")) return new IntType(typeName)
			if ("address" == typeName) return new AddressType
			if ("string" == typeName) return new StringType
			if ("bytes" == typeName) return new BytesType("bytes")
			if (typeName.startsWith("bytes")) return new Bytes32Type(typeName)
			throw new RuntimeException("Unknown type: [%s]".format(aTypeName))
		}

		abstract class ArrayType(_name: String) extends Type(_name) {
			protected val elementType: Type = {
				val idx1 = _name.indexOf("[")
				val substring = _name.substring(0, idx1)
				val idx2 = _name.indexOf("]", idx1)
				val subDim =
					if (idx2 + 1 == _name.length) {
						""
					} else {
						name.substring(idx2 + 1)
					}
				Type.getType(substring + subDim)
			}

			override def encode(value: Any): ImmutableBytes = {
				if (value.getClass.isArray) {
					val len = java.lang.reflect.Array.getLength(value)
					val seq = (0 until len).map {
						i => java.lang.reflect.Array.get(value, i)
					}.toSeq
					encodeSeq(seq)
				} else if (value.isInstanceOf[Seq[_]]) {
					encodeSeq(value.asInstanceOf[Seq[_]])
				} else {
					throw new RuntimeException("Illegal type: " + value.getClass)
				}
			}

			def encodeSeq(seq: Seq[_]): ImmutableBytes

		}
		object ArrayType {
			def getType(typeName: String): ArrayType = {
				val idx1 = typeName.indexOf("[")
				val idx2 = typeName.indexOf("]", idx1)
				if (idx1 + 1 == idx2) {
					new DynamicArrayType(typeName)
				} else {
					new StaticArrayType(typeName)
				}
			}
		}
		class StaticArrayType(_name: String) extends ArrayType(_name) {
			private val size = {
				val idx1 = _name.indexOf("[")
				val idx2 = _name.indexOf("]", idx1)
				val dim = _name.substring(idx1 + 1, idx2)
				dim.toInt
			}
			override def canonicalName = "%s[%d]".format(elementType.canonicalName, this.size)

			override def encodeSeq(seq: Seq[_]): ImmutableBytes = {
				val seqOfBytes = seq.map {
					each => this.elementType.encode(each)
				}
				seqOfBytes.foldLeft(ImmutableBytes.empty)((accum, each) => accum ++ each)
			}

			override def decode(encoded: ImmutableBytes): Any = throw new UnsupportedOperationException
			override def fixedSize: Int = this.elementType.fixedSize * this.size
		}

		class DynamicArrayType(_name: String) extends ArrayType(_name) {
			override def canonicalName = "%s[]".format(elementType.canonicalName)

			override def encodeSeq(seq: Seq[_]): ImmutableBytes = {
				val firstElem = IntType.encodeInt(seq.size)
				val seqOfBytes = seq.map {
					each => this.elementType.encode(each)
				}
				(firstElem +: seqOfBytes).foldLeft(ImmutableBytes.empty)((accum, each) => accum ++ each)
			}
			override def decode(encoded: ImmutableBytes): Any = throw new UnsupportedOperationException
			override val fixedSize: Int = -1
		}

		class BytesType(_name: String) extends Type(_name) {
			override def encode(value: Any): ImmutableBytes = {
				val original = value.asInstanceOf[Array[Byte]]
				val result = new Array[Byte](((original.length - 1) / 32 + 1) * 32)//padding to N * 32 bytes
				System.arraycopy(original, 0, result, 0, original.length)
				IntType.encodeInt(original.length) ++ ImmutableBytes(result)
			}
			override def decode(encoded: ImmutableBytes): Any = throw new UnsupportedOperationException
			override def fixedSize: Int = -1
		}

		class StringType extends BytesType("string") {
			override def encode(value: Any): ImmutableBytes = {
				super.encode(value.asInstanceOf[String].getBytes(StandardCharsets.UTF_8))
			}
			override def decode(encoded: ImmutableBytes): Any = throw new UnsupportedOperationException
		}

		class Bytes32Type(_name: String) extends Type(_name) {
			override def encode(value: Any): ImmutableBytes = {
				value match {
					case v if (v.isInstanceOf[Byte] || v.isInstanceOf[Short] || v.isInstanceOf[Int] || v.isInstanceOf[Long]) =>
						val bigInt = BigInt(v.toString)
						IntType.encodeInt(bigInt)
					case s: String =>
						val result = new Array[Byte](32)
						val bytes = s.getBytes(StandardCharsets.UTF_8)
						System.arraycopy(bytes, 0, result, 0, bytes.length)
						ImmutableBytes(result)
					case _ => ImmutableBytes.empty
				}
			}
			override def decode(encoded: ImmutableBytes): Any = encoded
		}

		class AddressType extends IntType("address") {
			override def encode(value: Any): ImmutableBytes = {
				val v =
					value match {
						case s: String =>
							if (!s.startsWith("0x")) {
								"0x" + s
							} else {
								s
							}
						case _ => value
					}
				val addr: ImmutableBytes = super.encode(v)
				(0 until 12).foreach {
					i => {
						if (addr(i) != 0) {
							throw new RuntimeException("Invalid Address: " + value)
						}
					}
				}
				addr
			}
		}

		class IntType(_name: String) extends Type(_name) {
			override def canonicalName: String = {
				if (this.name == "int") return "int256"
				if (this.name == "uint") return "uint256"
				super.canonicalName
			}

			override def encode(value: Any): ImmutableBytes = {
				val bigInt =
					value match {
						case s: String =>
							val laundered = s.trim.toLowerCase
							val (target, radix) =
								if (laundered.startsWith("0x")) {
									(laundered.substring(2), 16)
								} else if (laundered.contains("a") || laundered.contains("b") || laundered.contains("c") || laundered.contains("d") || laundered.contains("e") || laundered.contains("f")) {
									(laundered, 16)
								} else {
									(laundered, 10)
								}
							BigInt(target, radix)
						case v: BigInt => v
						case v if (v.isInstanceOf[Byte] || v.isInstanceOf[Short] || v.isInstanceOf[Int] || v.isInstanceOf[Long]) => BigInt(v.toString)
						case _ => throw new RuntimeException("Invalid value: " + value)
					}
				IntType.encodeInt(bigInt)
			}
			override def decode(encoded: ImmutableBytes): Any = encoded.toSignedBigInt
		}
		object IntType {
			def encodeInt(i: Int): ImmutableBytes = encodeInt(BigInt(i))
			def encodeInt(v: BigInt): ImmutableBytes = {
				val result = new Array[Byte](32)
				util.Arrays.fill(result, if (v.signum < 0) 0xFF.toByte else 0.toByte)
				val bytes = v.toByteArray
				System.arraycopy(bytes, 0, result, 32 - bytes.length, bytes.length)
				ImmutableBytes(result)
			}
		}
		class BoolType extends IntType("bool") {
			override def encode(value: Any): ImmutableBytes = {
				val v: Boolean = value.asInstanceOf[Boolean]
				super.encode(if (v) 1 else 0)
			}
			override def decode(encoded: ImmutableBytes): Any = {
				val result: Boolean = super.decode(encoded).asInstanceOf[BigInt].intValue() != 0
				result
			}
		}
	}

	class Param {
		private var name: String = ""
		def getName: String = this.name
		def setName(v: String): Unit = this.name = v

		private var paramType: Type = new StringType
		def getParamType: Type = this.paramType
		def setType(v: String): Unit = {
			this.paramType = Type.getType(v)
		}
	}

	sealed trait FunctionType
	case object FT_Constructor extends FunctionType
	case object FT_Function extends FunctionType

	class Function {

		private var constant: Boolean = false
		def setConstant(v: Boolean): Unit = this.constant = v
		def getConstant: Boolean = this.constant

		private var name: String = ""
		def setName(v: String): Unit = this.name = v
		def getName: String = this.name

		private var inputs: Array[Param] = Array.empty
		def setInputs(v: Array[Param]): Unit = this.inputs = v
		def getInputs: Array[Param] = this.inputs

		private var outputs: Array[Param] = Array.empty
		def setOutputs(v: Array[Param]): Unit = this.outputs = v
		def getOutputs: Array[Param] = this.outputs

		private var functionType: FunctionType = FT_Function
		def setType(v: String): Unit = {
			if (v.toLowerCase == "constructor") {
				this.functionType = FT_Constructor
			} else {
				this.functionType = FT_Function
			}
		}
		def getFunctionType: FunctionType = this.functionType

		def encode(args: Any*): ImmutableBytes = {
			if (this.inputs.length < args.length) {
				throw new RuntimeException("Too many arguments. %d < %d".format(this.inputs.length, args.length))
			}
			val (staticSize, dynamicCount): (Int, Int) =
				this.inputs.slice(0, args.length).foldLeft((0, 0)) {
					(accum, each) => {
						val (accumStaticSize, accumDynamicCount) = accum
						val eachSize = each.getParamType.fixedSize
						if (eachSize < 0) {
							(accumStaticSize + 32, accumDynamicCount + 1)
						} else {
							(accumStaticSize + eachSize, accumDynamicCount)
						}
					}
				}

			val seqOfBytes = new Array[ImmutableBytes](args.length + 1 + dynamicCount)
			seqOfBytes(0) = encodeSignature

			var currentDynamicPointer = staticSize
			var currentDynamicCount = 0
			for (i <- 0 until args.length) {
				if (inputs(i).getParamType.fixedSize < 0) {
					val dynamicBytes = inputs(i).getParamType.encode(args(i))
					seqOfBytes(i + 1) = IntType.encodeInt(currentDynamicPointer)
					seqOfBytes(args.length + 1 + currentDynamicCount) = dynamicBytes
					currentDynamicCount += 1
					currentDynamicPointer += dynamicBytes.length
				} else {
					val encodedBytes = inputs(i).getParamType.encode(args(i))
					seqOfBytes(i + 1) = encodedBytes
				}
			}
			seqOfBytes.foldLeft(ImmutableBytes.empty)((accum, each) => accum ++ each)
		}

		def decodeResult(encodedResult: ImmutableBytes): Seq[Any] = {
			if (1 < this.outputs.length) {
				throw new UnsupportedOperationException("Multiple return values not supported.")
			}
			if (outputs.isEmpty) {
				return Seq.empty
			}
			val retType = this.outputs.head.getParamType
			Seq(retType.decode(encodedResult))
		}

		def encodeSignature: ImmutableBytes = {
			val part =
				this.inputs.foldLeft(this.name + "(") {
					(accum, each) => accum + each.getParamType.canonicalName + ","
				}
			val signature =
				if (part.endsWith(",")) {
					part.substring(0, part.length - 1) + ")"
				} else {
					part + ")"
				}
			ImmutableBytes(util.Arrays.copyOfRange(DigestUtils.digest256(signature.getBytes), 0, 4))
		}
	}

	object Function {
		def fromJsonInterface(json: String): Function = {
			new ObjectMapper().readValue(json, (new Function).getClass)
		}
	}
}
