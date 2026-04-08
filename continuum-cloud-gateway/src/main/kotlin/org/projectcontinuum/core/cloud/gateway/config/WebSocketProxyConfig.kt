package org.projectcontinuum.core.cloud.gateway.config

import org.projectcontinuum.core.cloud.gateway.service.WorkbenchWebSocketProxyHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

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
