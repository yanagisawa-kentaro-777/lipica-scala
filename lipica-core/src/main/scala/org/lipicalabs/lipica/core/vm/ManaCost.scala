package org.lipicalabs.lipica.core.vm

/**
 *
 * @since 2015/10/25
 * @author YANAGISAWA, Kentaro
 */
object ManaCost {

	/** ゼロでないデータ１バイトにかかるフィー。 */
	val TX_NO_ZERO_DATA = 68
	/** ゼロであるデータ１バイトにかかるフィー。 */
	val TX_ZERO_DATA = 4
	/** １トランザクションにかかるフィー。 */
	val TRANSACTION = 21000

}
