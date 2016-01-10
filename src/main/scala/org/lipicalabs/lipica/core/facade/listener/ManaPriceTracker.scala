package org.lipicalabs.lipica.core.facade.listener

import org.lipicalabs.lipica.core.kernel.TransactionExecutionSummary

/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 20:07
 * YANAGISAWA, Kentaro
 */
class ManaPriceTracker extends LipicaListenerAdaptor {

	private val window = new Array[Long](512)
	private var index = this.window.length - 1
	private var filled = false

	private var calculatedValue: Long = 0

	override def onTransactionExecuted(summary: TransactionExecutionSummary): Unit = {
		this.synchronized {
			if (this.index < 0) {
				//一周した。
				index = this.window.length - 1
				this.filled = true
				//次回値を要求された時に、再計算する。
				this.calculatedValue = 0
			}
			//実績を記憶する。
			this.window(index) = summary.manaPrice.longValue()
			//インデックスを進める。
			this.index -= 1
		}
	}

	def getManaPrice: Long = {
		this.synchronized {
			if (!filled) {
				//まだサンプル数が足りないので、初期値を返しておく。
				ManaPriceTracker.DefaultValue
			} else {
				if (calculatedValue <= 0) {
					//今回の一周に基づく値を計算する。
					val copied = java.util.Arrays.copyOf(this.window, this.window.length)
					java.util.Arrays.sort(copied)
					//安い方から25%に当たる値を標準値とする。
					this.calculatedValue = copied(copied.length / 4)
				}
				this.calculatedValue
			}
		}
	}
}

object ManaPriceTracker {
	private val DefaultValue = 70000000000L
}