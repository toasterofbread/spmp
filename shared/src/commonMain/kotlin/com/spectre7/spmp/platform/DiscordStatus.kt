package com.spectre7.spmp.platform

expect class DiscordStatus {
    fun isSupported(): Boolean
    
    fun loginNeeded(): Boolean
    fun performLogin(playerProvider: () -> PlayerViewContext, callback: (success: Boolean) -> Unit)

    val enabled: Boolean
    fun enable()
    fun disable()
}
