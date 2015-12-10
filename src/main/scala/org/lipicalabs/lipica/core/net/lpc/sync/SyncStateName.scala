package org.lipicalabs.lipica.core.net.lpc.sync

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 19:57
 * YANAGISAWA, Kentaro
 */
sealed trait SyncStateName

case object Idle extends SyncStateName
case object HashRetrieving extends SyncStateName
case object BlockRetrieving extends SyncStateName

case object DoneHashRetrieving extends SyncStateName
case object BlocksLack extends SyncStateName

