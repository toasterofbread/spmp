package com.toasterofbread.spmp.platform.playerservice

open class ForwardingPlayerService(
    protected val player: PlayerService
): PlayerService by player
