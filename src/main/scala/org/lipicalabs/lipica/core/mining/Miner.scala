package org.lipicalabs.lipica.core.mining

import java.util.concurrent.atomic.AtomicBoolean

import org.lipicalabs.lipica.core.kernel.{BlockHeader, Block}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.validator.block_header_rules.ProofOfWorkRule
import org.slf4j.LoggerFactory


/**
 * 比較的単純な Proof of Work による mining を実行するクラスです。
 *
 * PoWによるminingは、
 * ・ブロックチェーンが将来にわたって正統性を維持し続けられることの保証。
 * ・通貨発行益の獲得競争。
 * の２つの目的を持ちます。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/24 15:22
 * YANAGISAWA, Kentaro
 */
class Miner {
	import Miner._

	private val stopRef = new AtomicBoolean(false)

	/**
	 * 指定された難度を満たすように、渡されたブロックにnonce値を設定します。
	 *
	 * PoW(H, n) ≡ BE(SHA3(SHA3(RLP(H!n)) ◦ n))
	 *
	 * where:
	 * RLP(H!n) is the RLP encoding of the block header H, not including the
	 * final nonce component;
	 * SHA3 is the SHA3 hash function accepting an arbitrary length series of
	 * bytes and evaluating to a series of 32 bytes (i.e. 256-bit);
	 * n is the nonce, a series of 32 bytes;
	 * o is the series concatenation operator;
	 * BE(X) evaluates to the value equal to X when interpreted as a
	 * big-endian-encoded integer.
	 */
	def mine(newBlock: Block, difficulty: ImmutableBytes): Boolean = {
		this.stopRef.set(false)

		val target = ProofOfWorkRule.getProofOfWorkBoundary(difficulty)

		//yellow paper の 式40および41を参照。
		val newManaLimit = 125000L max (newBlock.manaLimit.toPositiveBigInt.longValue * (1024 - 1) + (newBlock.manaUsed * 6 / 5)) / 1024
		newBlock.blockHeader.manaLimit = ImmutableBytes.asSignedByteArray(BigInt(newManaLimit))
		val hash = DigestUtils.digest256(newBlock.encodeWithoutNonce.toByteArray)
		val testNonce = new Array[Byte](32)

		logger.info("<Miner> Target is %s".format(target))
		while (!this.stopRef.get && increment(testNonce)) {
			if (logger.isDebugEnabled && (testNonce(31) == 0) && (testNonce(30) == 0)) {
				logger.debug("<Miner> Mining %s".format(ImmutableBytes(testNonce)))
			}
			if (testNonce(31) == 0) {
				Thread.sleep(10L)
			}

			val wrappedTestNonce = ImmutableBytes(testNonce)
			newBlock.nonce = wrappedTestNonce
			val powValue = newBlock.blockHeader.calculateProofOfWorkValue

			if (powValue.compareTo(target) <= 0) {
				logger.info("<Miner> Mined! %s < %s (nonce=%s)".format(powValue, target, ImmutableBytes(testNonce)))
				//println("<Miner> Mined! %s < %s (nonce=%s)".format(result, target, ImmutableBytes(testNonce)))
				return true
			}
		}
		false
	}

	def stop(): Unit = this.stopRef.set(true)

}

object Miner {
	private val logger = LoggerFactory.getLogger("mining")

	def increment(bytes: Array[Byte]): Boolean = {
		val startIndex = 0
		bytes.indices.reverse.foreach {
			i => {
				bytes(i) = (bytes(i) + 1).toByte
				if (bytes(i) != 0) {
					return true
				} else if (i == startIndex) {
					return false
				}
			}
		}
		true
	}
}