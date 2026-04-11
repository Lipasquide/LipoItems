package dev.lipasquide.lipoitems.server

import com.sun.net.httpserver.HttpServer
import dev.lipasquide.lipoitems.LipoItems
import java.io.File
import java.net.InetSocketAddress

class PackServer(private val plugin: LipoItems) {
    private var server: HttpServer? = null

    fun start() {
        val port = plugin.config.getInt("server.port", 8080)
        try {
            server = HttpServer.create(InetSocketAddress(port), 0)
            server?.createContext("/pack.zip") { exchange ->
                val packFile = File(plugin.dataFolder, "generated/pack.zip")
                if (!packFile.exists()) {
                    exchange.sendResponseHeaders(404, -1)
                    exchange.close()
                    return@createContext
                }

                exchange.sendResponseHeaders(200, packFile.length())
                packFile.inputStream().use { it.copyTo(exchange.responseBody) }
                exchange.close()
            }
            server?.executor = null
            server?.start()
            plugin.logger.info("Resource pack server started on port $port")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start resource pack server: ${e.message}")
        }
    }

    fun stop() {
        server?.stop(0)
    }
}
