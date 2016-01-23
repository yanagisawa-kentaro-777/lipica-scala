package org.lipicalabs.lipica.core.vm

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.annotation.tailrec
import scala.util.Random

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class VMWordTest extends Specification {
	sequential

	private val RANDOM_CASES = 100

	"last 20" should {
		"be right" in {
			val word = VMWord((0 until 32).map(_.toByte).toArray)
			word.data.length mustEqual 32

			val bytes = word.last20Bytes.bytes
			bytes.length mustEqual 20
			bytes(0) mustEqual (32 - 20)
			bytes(19) mustEqual 31
		}
	}

	"strip zeros (1)" should {
		"be right" in {
			val word = VMWord((0 until 32).map(_.toByte).toArray)
			word.data.length mustEqual 32
			word.getDataWithoutLeadingZeros.length mustEqual 31
			word.occupiedBytes mustEqual 31
			word.toPrefixString mustEqual "010203"
		}
	}

	"strip zeros (2)" should {
		"be right" in {
			val word = VMWord((1 until 33).map(_.toByte).toArray)
			word.data.length mustEqual 32
			word.getDataWithoutLeadingZeros.length mustEqual 32
			word.occupiedBytes mustEqual 32
			word.toPrefixString mustEqual "010203"
		}
	}

	"strip zeros (3)" should {
		"be right" in {
			val word = VMWord(0)
			word.data.length mustEqual 32
			word.getDataWithoutLeadingZeros.length mustEqual 0
			word.occupiedBytes mustEqual 0
			(word == VMWord.Zero) mustEqual true
			word.hashCode mustEqual VMWord.Zero.hashCode
			word.toPrefixString mustEqual ""
		}
	}

	"strip zeros (4)" should {
		"be right" in {
			val word = VMWord(1)
			word.data.length mustEqual 32
			word.getDataWithoutLeadingZeros.length mustEqual 1
			word.occupiedBytes mustEqual 1
			(word == VMWord.One) mustEqual true
			word.toPrefixString mustEqual "01"
		}
	}

	"instance creation (1)" should {
		"be right" in {
			val zero = VMWord(null.asInstanceOf[ImmutableBytes])
			zero.isZero mustEqual true
		}
	}

	"instance creation (2)" should {
		"be right" in {
			val word = VMWord(ImmutableBytes((0 until 32).map(_.toByte).toArray))
			word.data.indices.foreach {i => {
				word.data(i) mustEqual i
			}}
			word.data.length mustEqual 32
		}
	}

	"instance creation (3)" should {
		"be right" in {
			val word = VMWord(ImmutableBytes.fromOneByte(1))
			word.data.length mustEqual 32
			word.data(31) mustEqual 1
			word.getDataWithoutLeadingZeros.length mustEqual 1
		}
	}

	"comparison" should {
		"be right" in {
			val zero = VMWord(0)
			zero.compareTo(VMWord.Zero) mustEqual 0
			zero.compareTo(VMWord.One) mustEqual -1
			VMWord.One.compareTo(zero) mustEqual 1
		}
	}

	"add simple" should {
		"be right" in {
			val word0 = VMWord(0)
			val word1 = VMWord(1)
			val word2 = VMWord(2)
			(word1 + word2).intValue mustEqual 3
			(word0 + word1).intValue mustEqual 1
			(word2 + word0).intValue mustEqual 2
		}
	}

	"and random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (And): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = random.nextLong()
					val word1 = VMWord(value1)
					val value2 = random.nextLong()
					val word2 = VMWord(value2)
					(word1 & word2).longValue mustEqual (value1 & value2)
					//println("[And] %,d == %,d".format((word1 & word2).longValue, value1 & value2))
				}
			}
			ok
		}
	}

	"or random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Or): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = random.nextLong()
					val word1 = VMWord(value1)
					val value2 = random.nextLong()
					val word2 = VMWord(value2)
					(word1 | word2).longValue mustEqual (value1 | value2)
					//println("[Or] %,d == %,d".format((word1 | word2).longValue, value1 | value2))
				}
			}
			ok
		}
	}

	"xor random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Xor): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = random.nextLong()
					val word1 = VMWord(value1)
					val value2 = random.nextLong()
					val word2 = VMWord(value2)
					(word1 ^ word2).longValue mustEqual (value1 ^ value2)
					//println("[Xor] %,d == %,d".format((word1 ^ word2).longValue, value1 ^ value2))
				}
			}
			ok
		}
	}

	"ones' complement random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (1s' complement): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value = Math.abs(random.nextLong())
					val word = VMWord(value)
					(~word).longValue mustEqual ~value
					//println("[1s' compl] %,d == %,d".format((~word).longValue, ~value))
				}
			}
			ok
		}
	}

	"add random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Add): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = random.nextLong()
					val word1 = VMWord(value1)
					val value2 = random.nextLong()
					val word2 = VMWord(value2)
					(word1 + word2).longValue mustEqual (value1 + value2)
					//println("[Add] %,d == %,d".format((word1 + word2).longValue, value1 + value2))
				}
			}
			ok
		}
	}

	"sub random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Sub): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = random.nextLong()
					val value2 = random.nextLong()

					val word1 = VMWord(value1.max(value2))
					val word2 = VMWord(value2.min(value1))
					(word1 - word2).longValue mustEqual (value1.max(value2) - value2.min(value1))
					//println("[Sub] %,d == %,d".format((word1 - word2).longValue, value1 - value2))
				}
			}
			ok
		}
	}

	"mul random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Mul): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = random.nextLong()
					val word1 = VMWord(value1)
					val value2 = random.nextLong()
					val word2 = VMWord(value2)
					(word1 * word2).longValue mustEqual (value1 * value2)
					//println("[Mul] %,d == %,d".format((word1 * word2).longValue, value1 * value2))
				}
			}
			ok
		}
	}

	@tailrec
	private def generateNonZeroLong(random: Random): Long = {
		val result = Math.abs(random.nextLong())
		if (result == 0L) {
			generateNonZeroLong(random)
		} else {
			result
		}
	}

	"div random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Div): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = Math.abs(random.nextLong())
					val word1 = VMWord(value1)
					val value2 = generateNonZeroLong(random)
					val word2 = VMWord(value2)
					(word1 / word2).longValue mustEqual (value1 / value2)
					//println("[Div] %,d == %,d".format((word1 / word2).longValue, value1 / value2))
				}
			}
			ok
		}
	}

	"mod random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Mod): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = Math.abs(random.nextLong())
					val word1 = VMWord(value1)
					val value2 = generateNonZeroLong(random)
					val word2 = VMWord(value2)
					(word1 % word2).longValue mustEqual (value1 % value2)
					//println("[Mod] %,d == %,d".format((word1 % word2).longValue, value1 % value2))
				}
			}
			ok
		}
	}

	"exp random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (Exp): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = Math.abs(random.nextInt(Int.MaxValue / 4))
					val word1 = VMWord(value1)
					val value2 = Math.abs(random.nextInt(3))
					val word2 = VMWord(value2)
					(word1 exp word2).longValue mustEqual BigInt(value1).pow(value2).longValue
					//println("[Exp] %,d == %,d".format((word1 exp word2).longValue, BigInt(value1).pow(value2).longValue()))
				}
			}
			ok
		}
	}

	"add mod random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (AddMod): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = Math.abs(random.nextInt((Int.MaxValue / 2) - 1))
					val word1 = VMWord(value1)
					val value2 = Math.abs(random.nextInt((Int.MaxValue / 2) - 1))
					val word2 = VMWord(value2)
					val value3 = Math.abs(random.nextInt(Int.MaxValue - 1)) + 1
					val word3 = VMWord(value3)
					word1.addMod(word2, word3).longValue mustEqual (BigInt(value1) + BigInt(value2)) % BigInt(value3)
					//println("[AddMod] %,d == %,d".format(word1.addMod(word2, word3).longValue, (BigInt(value1) + BigInt(value2)) % BigInt(value3)))
				}
			}
			ok
		}
	}

	"mul mod random" should {
		"be right" in {
			val seed = System.currentTimeMillis
			println("Seed (MulMod): %,d".format(seed))
			val random = new Random(seed)
			(0 until RANDOM_CASES).foreach {
				_ => {
					val value1 = Math.abs(random.nextInt((Int.MaxValue / 2) - 1))
					val word1 = VMWord(value1)
					val value2 = Math.abs(random.nextInt((Int.MaxValue / 2) - 1))
					val word2 = VMWord(value2)
					val value3 = Math.abs(random.nextInt(Int.MaxValue - 1)) + 1
					val word3 = VMWord(value3)
					word1.mulMod(word2, word3).longValue mustEqual (BigInt(value1) * BigInt(value2)) % BigInt(value3)
					//println("[MulMod] %,d == %,d".format(word1.mulMod(word2, word3).longValue, (BigInt(value1) * BigInt(value2)) % BigInt(value3)))
				}
			}
			ok
		}
	}

	"mul overflow" should {
		"be right" in {
			val a = new Array[Byte](32)
			a(30) = 0x1 // 0x0000000000000000000000000000000000000000000000000000000000000100
			val b = new Array[Byte](32)
			b(0) = 0x1 // 0x1000000000000000000000000000000000000000000000000000000000000000

			val x = VMWord(a)
			val y = VMWord(b)
			val result = x * y
			result.data.length mustEqual 32
		}
	}

	"div large" should {
		"be right" in {
			val a = new Array[Byte](32)
			a(30) = 0x01
			a(31) = 0x2c
			// 0x000000000000000000000000000000000000000000000000000000000000012c

			val b = new Array[Byte](32)
			b(31) = 0x0f
			// 0x000000000000000000000000000000000000000000000000000000000000000f

			val x = VMWord(a)
			val y = VMWord(b)
			val result = x / y
			result.isHex("0000000000000000000000000000000000000000000000000000000000000014") mustEqual true
		}
	}

	"div by zero" should {
		"be right" in {
			val a = new Array[Byte](32)
			a(30) = 0x05
			// 0x0000000000000000000000000000000000000000000000000000000000000500

			val b = new Array[Byte](32)

			val x = VMWord(a)
			val y = VMWord(b)
			val result = x / y
			result.isZero mustEqual true
		}
	}

	"mod by zero" should {
		"be right" in {
			val a = new Array[Byte](32)
			a(30) = 0x05
			// 0x0000000000000000000000000000000000000000000000000000000000000500

			val b = new Array[Byte](32)

			val x = VMWord(a)
			val y = VMWord(b)
			val result = x % y
			result.isZero mustEqual true
		}
	}

	"sdiv negative" should {
		"be right" in {
			val a = Hex.decodeHex("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed4".toCharArray)
			//-300

			val b = new Array[Byte](32)
			b(31) = 0x0f

			val x = VMWord(a)
			val y = VMWord(b)
			val result = x sDiv y

			result.toString mustEqual "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffec"
		}
	}

	"sign extend (1)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("f2".toCharArray))
			val result = x.signExtend(0)

			val expected : String = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff2"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (2)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("f2".toCharArray))
			val result = x.signExtend(1)

			val expected : String = "00000000000000000000000000000000000000000000000000000000000000f2"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (3)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("0f00ab".toCharArray))
			val result = x.signExtend(1)

			val expected : String = "00000000000000000000000000000000000000000000000000000000000000ab"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (4)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("ffff".toCharArray))
			val result = x.signExtend(1)

			val expected : String = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (5)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("ffffffff".toCharArray))
			val result = x.signExtend(3)

			val expected : String = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (6)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("ab02345678".toCharArray))
			val result = x.signExtend(3)

			val expected : String = "0000000000000000000000000000000000000000000000000000000002345678"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (7)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("ab82345678".toCharArray))
			val result = x.signExtend(3)

			val expected : String = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff82345678"
			result.toHexString mustEqual expected
		}
	}

	"sign extend (8)" should {
		"be right" in {
			val x  = VMWord(Hex.decodeHex("ff34567882345678823456788234567882345678823456788234567882345678".toCharArray))
			val result = x.signExtend(30)

			val expected : String = "0034567882345678823456788234567882345678823456788234567882345678"
			result.toHexString mustEqual expected
		}
	}

	"sign extend exception (1)" should {
		"be right" in {
			try {
				val x  = VMWord(Hex.decodeHex("ff34567882345678823456788234567882345678823456788234567882345678".toCharArray))
				x.signExtend(-1)
				ko
			} catch {
				case e: IndexOutOfBoundsException => ok
				case _: Throwable => ko
			}
		}
	}

	"sign extend exception (2)" should {
		"be right" in {
			try {
				val x  = VMWord(Hex.decodeHex("ff34567882345678823456788234567882345678823456788234567882345678".toCharArray))
				x.signExtend(32)
				ko
			} catch {
				case e: IndexOutOfBoundsException => ok
				case _: Throwable => ko
			}
		}
	}

	"overflow" should {
		"be right" in {
			val v1 = VMWord(7)
			v1.intValueSafe mustEqual 7
			v1.longValueSafe mustEqual 7L

			val v2 = VMWord((0 until 32).map(_ => 0xff.toByte).toArray)
			v2.intValueSafe mustEqual Int.MaxValue
			v2.longValueSafe mustEqual Long.MaxValue
		}
	}

}
