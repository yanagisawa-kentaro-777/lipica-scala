package org.lipicalabs.lipica.core.base.genesis

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:42
 * YANAGISAWA, Kentaro
 */
class AllocatedAccount {

	var balance: String = ""

	def getBalance: String = balance

	def setBalance(balance: String) {
		this.balance = balance
	}

}
