package org.lipicalabs.lipica.core.trie

import java.util.concurrent.atomic.AtomicBoolean
import org.lipicalabs.lipica.core.utils.ImmutableBytes

class CachedNode(val encodedBytes: ImmutableBytes, _dirty: Boolean) {
	/**
	 * 永続化されていない更新があるか否か。
	 */
	private val isDirtyRef = new AtomicBoolean(_dirty)
	def isDirty = this.isDirtyRef.get
	def isDirty(value: Boolean): CachedNode = {
		this.isDirtyRef.set(value)
		this
	}
}

