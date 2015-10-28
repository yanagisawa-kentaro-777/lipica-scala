package org.lipicalabs.lipica.core.base

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ByteUtils
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class TransactionTest extends Specification {
	sequential

	private val RLP_ENCODED_RAW_TX: String = "e88085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc1000080"
	private val RLP_ENCODED_UNSIGNED_TX: String = "eb8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc1000080808080"
	//private val HASH_TX: String = "328ea6d24659dec48adea1aced9a136e5ebdf40258db30d1b1d97ed2b74be34e"//TODO
	private val HASH_UNSIGNED_TX = "b747c9318ba950fb2a002683fe9d8874eb17cad6e98831f2ae08a9e5c1753710"
	private val HASH_SIGNED_TX = "5d3466b457f3480945474de8e2df3c01ceaa55a12d0347d2e17a3f3444651f86"
	private val RLP_ENCODED_SIGNED_TX: String = "f86b8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc10000801ba0eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4a014a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
	private val KEY: String = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4"
	private val testNonce: Array[Byte] = Hex.decodeHex("".toCharArray)
	private val testManaPrice: Array[Byte] = ByteUtils.asUnsignedByteArray(BigInt(1000000000000L))
	private val testManaLimit: Array[Byte] = ByteUtils.asUnsignedByteArray(BigInt(10000))
	private val testReceiveAddress: Array[Byte] = Hex.decodeHex("13978aee95f38490e9769c39b2773ed763d9cd5f".toCharArray)
	private val testValue: Array[Byte] = ByteUtils.asUnsignedByteArray(BigInt(10000000000000000L))
	private val testData: Array[Byte] = Hex.decodeHex("".toCharArray)
	private val testInit: Array[Byte] = Hex.decodeHex("".toCharArray)

	"test transaction from signed RBAC" should {
		"be right" in {
			val txSigned = Transaction(Hex.decodeHex(RLP_ENCODED_SIGNED_TX.toCharArray))
			Hex.encodeHexString(txSigned.getHash) mustEqual HASH_SIGNED_TX
			Hex.encodeHexString(txSigned.getEncoded) mustEqual RLP_ENCODED_SIGNED_TX
			BigInt(1, txSigned.getNonce) mustEqual BigInt(0)
			BigInt(1, txSigned.getManaPrice) mustEqual BigInt(1, testManaPrice)
			BigInt(1, txSigned.getManaLimit) mustEqual BigInt(1, testManaLimit)
			Hex.encodeHexString(txSigned.getReceiveAddress) mustEqual Hex.encodeHexString(testReceiveAddress)
			BigInt(1, txSigned.getValue) mustEqual BigInt(1, testValue)
			txSigned.getData.isEmpty mustEqual true
			txSigned.getSignature.v mustEqual 27
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(txSigned.getSignature.r)) mustEqual "eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(txSigned.getSignature.s)) mustEqual "14a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
		}
	}

	"test transaction from unsigned RBAC" should {
		"be right" in {
			val tx = Transaction(Hex.decodeHex(RLP_ENCODED_UNSIGNED_TX.toCharArray))
			Hex.encodeHexString(tx.getHash) mustEqual HASH_UNSIGNED_TX
			Hex.encodeHexString(tx.getEncoded) mustEqual RLP_ENCODED_UNSIGNED_TX
			tx.sign(Hex.decodeHex(KEY.toCharArray))
			Hex.encodeHexString(tx.getHash) mustEqual HASH_SIGNED_TX
			Hex.encodeHexString(tx.getEncoded) mustEqual RLP_ENCODED_SIGNED_TX

			BigInt(1, tx.getNonce) mustEqual BigInt(0)
			BigInt(1, tx.getManaPrice) mustEqual BigInt(1, testManaPrice)
			BigInt(1, tx.getManaLimit) mustEqual BigInt(1, testManaLimit)
			Hex.encodeHexString(tx.getReceiveAddress) mustEqual Hex.encodeHexString(testReceiveAddress)
			BigInt(1, tx.getValue) mustEqual BigInt(1, testValue)
			tx.getData.isEmpty mustEqual true

			tx.getSignature.v mustEqual 27
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.r)) mustEqual "eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.s)) mustEqual "14a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
		}
	}

	"test new transaction (1)" should {
		"be right" in {
			val tx = Transaction(testNonce, testManaPrice, testManaLimit, testReceiveAddress, testValue, testData)

			BigInt(1, tx.getNonce) mustEqual BigInt(0)
			BigInt(1, tx.getManaPrice) mustEqual BigInt(1, testManaPrice)
			BigInt(1, tx.getManaLimit) mustEqual BigInt(1, testManaLimit)
			Hex.encodeHexString(tx.getReceiveAddress) mustEqual Hex.encodeHexString(testReceiveAddress)
			BigInt(1, tx.getValue) mustEqual BigInt(1, testValue)
			tx.getData.isEmpty mustEqual true
			(tx.getSignature eq null) mustEqual true

			Hex.encodeHexString(tx.getEncodedRaw) mustEqual RLP_ENCODED_RAW_TX
			Hex.encodeHexString(tx.getHash) mustEqual HASH_UNSIGNED_TX
			Hex.encodeHexString(tx.getEncoded) mustEqual RLP_ENCODED_UNSIGNED_TX

			tx.sign(Hex.decodeHex(KEY.toCharArray))

			Hex.encodeHexString(tx.getHash) mustEqual HASH_SIGNED_TX
			Hex.encodeHexString(tx.getEncoded) mustEqual RLP_ENCODED_SIGNED_TX
			tx.getSignature.v mustEqual 27
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.r)) mustEqual "eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.s)) mustEqual "14a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
		}
	}

	"test new transaction (2)" should {
		"be right" in {
			val privKeyBytes = Hex.decodeHex("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4".toCharArray)
			val RLP_TX_UNSIGNED = "eb8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc1000080808080"
			val RLP_TX_SIGNED = "f86b8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc10000801ba0eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4a014a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
			val HASH_TX_UNSIGNED = "b747c9318ba950fb2a002683fe9d8874eb17cad6e98831f2ae08a9e5c1753710"
			val HASH_TX_SIGNED = "5d3466b457f3480945474de8e2df3c01ceaa55a12d0347d2e17a3f3444651f86"

			val nonce = ByteUtils.asUnsignedByteArray(BigInt(0))
			val manaPrice = Hex.decodeHex("e8d4a51000".toCharArray)
			val mana = Hex.decodeHex("2710".toCharArray)
			val receiveAddress = Hex.decodeHex("13978aee95f38490e9769c39b2773ed763d9cd5f".toCharArray)
			val value = Hex.decodeHex("2386f26fc10000".toCharArray)
			val data = Array.emptyByteArray

			val tx = Transaction(nonce, manaPrice, mana, receiveAddress, value, data)

			Hex.encodeHexString(tx.getEncoded) mustEqual RLP_TX_UNSIGNED
			Hex.encodeHexString(tx.getHash) mustEqual HASH_TX_UNSIGNED

			tx.sign(privKeyBytes)

			Hex.encodeHexString(tx.getEncoded) mustEqual RLP_TX_SIGNED
			Hex.encodeHexString(tx.getHash) mustEqual HASH_TX_SIGNED
		}
	}

	//TODO use https://github.com/ethereum/tests/blob/develop/TransactionTests/ttTransactionTest.json test cases.
	"test (1)" should {
		"be right" in {
			val tx = Transaction(Hex.decodeHex("f85f800182520894000000000000000000000000000b9331677e6ebf0a801ca098ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4aa08887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3".toCharArray))

			Hex.encodeHexString(tx.getSender) mustEqual "31bb58672e8bf7684108feeacf424ab62b873824"
			tx.getData.isEmpty mustEqual true
			Hex.encodeHexString(tx.getManaLimit) mustEqual "5208"
			Hex.encodeHexString(tx.getManaPrice) mustEqual "01"
			Hex.encodeHexString(tx.getNonce) mustEqual "00"
			Hex.encodeHexString(tx.getReceiveAddress) mustEqual "000000000000000000000000000b9331677e6ebf"
			Hex.encodeHexString(tx.getValue) mustEqual "0a"
			Hex.encodeHexString(Array(tx.getSignature.v)) mustEqual "1c"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.r)) mustEqual "98ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4a"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.s)) mustEqual "8887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3"
		}
	}

	"test (2)" should {
		"be right" in {
			val tx = Transaction(Hex.decodeHex("f86d80018259d894095e7baea6a6c7c4c2dfeb977efac326af552d870a8e0358ac39584bc98a7c979f984b031ba048b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353a0efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804".toCharArray))

			Hex.encodeHexString(tx.getSender) mustEqual "ce26839c9bd0e87e38897bb97fca8b340fd12a53"
			Hex.encodeHexString(tx.getData) mustEqual "0358ac39584bc98a7c979f984b03"
			Hex.encodeHexString(tx.getManaLimit) mustEqual "59d8"
			Hex.encodeHexString(tx.getManaPrice) mustEqual "01"
			Hex.encodeHexString(tx.getNonce) mustEqual "00"
			Hex.encodeHexString(tx.getReceiveAddress) mustEqual "095e7baea6a6c7c4c2dfeb977efac326af552d87"
			Hex.encodeHexString(tx.getValue) mustEqual "0a"
			Hex.encodeHexString(Array(tx.getSignature.v)) mustEqual "1b"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.r)) mustEqual "48b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.s)) mustEqual "efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"
		}
	}

	"test (3)" should {
		"be right" in {
			val tx = Transaction(Hex.decodeHex("f87c80018261a894095e7baea6a6c7c4c2dfeb977efac326af552d870a9d00000000000000000000000000010000000000000000000000000000001ba048b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353a0efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804".toCharArray))

			Hex.encodeHexString(tx.getSender) mustEqual "8131688854fe0dca411aa19572a01fe3e3e4fa74"
			Hex.encodeHexString(tx.getData) mustEqual "0000000000000000000000000001000000000000000000000000000000"//TODO doubt
			Hex.encodeHexString(tx.getManaLimit) mustEqual "61a8"
			Hex.encodeHexString(tx.getManaPrice) mustEqual "01"
			Hex.encodeHexString(tx.getNonce) mustEqual "00"
			Hex.encodeHexString(tx.getReceiveAddress) mustEqual "095e7baea6a6c7c4c2dfeb977efac326af552d87"
			Hex.encodeHexString(tx.getValue) mustEqual "0a"
			Hex.encodeHexString(Array(tx.getSignature.v)) mustEqual "1b"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.r)) mustEqual "48b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.s)) mustEqual "efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"
		}
	}

	"test (4)" should {
		"be right" in {
			val tx = Transaction(Hex.decodeHex("f87c80018261a894095e7baea6a6c7c4c2dfeb977efac326af552d870a9d00100000000000000000000000000000000000000000000000000000001ba048b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353a0efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804".toCharArray))

			Hex.encodeHexString(tx.getSender) mustEqual "ead53a9560ea38feb0bc2cad8ef65e5d8f990fc1"
			Hex.encodeHexString(tx.getData) mustEqual "0010000000000000000000000000000000000000000000000000000000"//TODO doubt
			Hex.encodeHexString(tx.getManaLimit) mustEqual "61a8"
			Hex.encodeHexString(tx.getManaPrice) mustEqual "01"
			Hex.encodeHexString(tx.getNonce) mustEqual "00"
			Hex.encodeHexString(tx.getReceiveAddress) mustEqual "095e7baea6a6c7c4c2dfeb977efac326af552d87"
			Hex.encodeHexString(tx.getValue) mustEqual "0a"
			Hex.encodeHexString(Array(tx.getSignature.v)) mustEqual "1b"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.r)) mustEqual "48b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.getSignature.s)) mustEqual "efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"
		}
	}

	//TODO 形を変えて戻して同じであることを確認するテスト。

}
