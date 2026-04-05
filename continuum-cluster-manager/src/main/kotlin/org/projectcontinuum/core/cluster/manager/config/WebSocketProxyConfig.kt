package org.projectcontinuum.core.cluster.manager.config

import org.projectcontinuum.core.cluster.manager.service.WorkbenchWebSocketProxyHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * Configures WebSocket support for the workbench reverse proxy.
 *
 * Registers a WebSocket handler at /api/v1/workbench/ ** /open/ ** that
 * proxies WebSocket connections to the target workbench instance.
 */
@Configuration
@EnableWebSocket
class WebSocketProxyConfig(
  private val webSocketProxyHandler: WorkbenchWebSocketProxyHandler
) : WebSocketConfigurer {

  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    registry
      .addHandler(webSocketProxyHandler, "/api/v1/workbench/*/open/**")
      .setAllowedOrigins("*")
  }
}

