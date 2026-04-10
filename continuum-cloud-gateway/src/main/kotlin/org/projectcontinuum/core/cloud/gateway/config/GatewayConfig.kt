package org.projectcontinuum.core.cloud.gateway.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.net.URI
import java.util.function.Function

// Spring Cloud Gateway Server MVC configuration.
//
// Static routes for proxying to backend services.
// The dynamic workbench proxy routing (/workbench/{instanceName}/open)
// is handled by WorkbenchProxyController and WorkbenchWebSocketProxyHandler because
// the target URL must be resolved dynamically from the database at request time.
@Configuration
class GatewayConfig(
  @Value("\${CONTINUUM_API_SERVER_URL:http://localhost:8081}")
  private val apiServerUrl: String,

  @Value("\${CONTINUUM_CLUSTER_MANAGER_URL:http://localhost:8082}")
  private val clusterManagerUrl: String,
) {

  @Bean
  fun apiServerRoute(): RouterFunction<ServerResponse> =
    route("api-server")
      .route(path("/api-server/**"), http(apiServerUrl))
      .before(preserveRawQueryString())
      .before(stripPrefix(1))
      .build()

  @Bean
  fun clusterManagerRoute(): RouterFunction<ServerResponse> =
    route("cluster-manager")
      .route(path("/cluster-manager/**"), http(clusterManagerUrl))
      .before(preserveRawQueryString())
      .before(stripPrefix(1))
      .build()

  // Before filters run in reverse registration order: stripPrefix runs first, then this.
  // After stripPrefix modifies the gateway request URI, this filter replaces the query
  // portion with the raw (percent-encoded) query string from the original servlet request.
  // This prevents Spring's URI handling from decoding %26 to & in query parameters.
  private fun preserveRawQueryString(): Function<ServerRequest, ServerRequest> =
    Function { request ->
      val rawQuery = request.servletRequest().queryString ?: return@Function request

      // Get the URI that stripPrefix set via MvcUtils
      val gatewayUri = MvcUtils.getRequestUrl(request)
      if (gatewayUri == null) return@Function request

      // Rebuild the URI with the raw query string to preserve percent-encoding
      val correctedUri = URI(
        "${gatewayUri.scheme}://${gatewayUri.authority}${gatewayUri.rawPath}?${rawQuery}"
      )
      MvcUtils.setRequestUrl(request, correctedUri)
      request
    }

  private fun path(pattern: String) =
    org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path(pattern)
}
