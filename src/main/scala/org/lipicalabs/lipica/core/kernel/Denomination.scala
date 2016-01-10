package org.lipicalabs.lipica.core.kernel

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 10:56
 * YANAGISAWA, Kentaro
 */
sealed trait Denomination {

	def value: BigInt

	def longValue = value.longValue()

}

object Denomination {

	private val Ten = BigInt(10)
	private def powerOf10(n: Int): BigInt = Ten pow n

	case object LPC extends Denomination {
		override val value = powerOf10(0)
	}

	case object TLPC extends Denomination {
		override val value = powerOf10(12)
	}

	case object PLPC extends Denomination {
		override val value = powerOf10(15)
	}

	case object ELPC extends Denomination {
		override val value = powerOf10(18)
	}

	def toHumanReadableString(amount: BigInt): String = {
		if (ELPC.value <= amount) {
			"%.4f ELPC".format((amount / ELPC.value).doubleValue())
		} else if (PLPC.value <= amount) {
			"%.4f PLPC".format((amount / PLPC.value).doubleValue())
		} else if (TLPC.value <= amount) {
			"%.4f TLPC".format((amount / TLPC.value).doubleValue())
		} else {
			"%,d LPC".format(amount)
		}
	}
}
