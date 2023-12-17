package com.toasterofbread.spmp.platform.playerservice

interface ClientServerPlayerService: PlayerService {
    data class ServerInfo(
        val ip: String,
        val port: Int,
        val protocol: String
    ) {
        fun getUrl(): String = "${protocol}://${ip}:$port"
    }
    
    val connected_server: ServerInfo?
    
    suspend fun getPeers(): Result<List<SpMsClientInfo>>
}
