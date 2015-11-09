package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:03
 * YANAGISAWA, Kentaro
 */
class AccountState(_bytes: ImmutableBytes) {

	def this() = this(null)

	//TODO

	def isDeleted: Boolean = ???

	def addToBalance(value: BigInt): BigInt = ???

	def encode: ImmutableBytes = ???

	def codeHash: ImmutableBytes = ???

	def codeHash_=(v: ImmutableBytes): Unit = ???

	def balance: BigInt = ???

	def nonce: BigInt = ???
	def nonce_=(v: BigInt): Unit = ???
	def incrementNonce(): Unit = ???

	def stateRoot: ImmutableBytes = ???
	def stateRoot_=(v: ImmutableBytes): Unit = ???

	override def clone: AccountState = ???

}
