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
      .before(stripPrefix(1))
      .before(::preserveRawQueryString)
      .build()

  @Bean
  fun clusterManagerRoute(): RouterFunction<ServerResponse> =
    route("cluster-manager")
      .route(path("/cluster-manager/**"), http(clusterManagerUrl))
      .before(stripPrefix(1))
      .before(::preserveRawQueryString)
      .build()

  // Preserves the raw (percent-encoded) query string from the original request.
  // Spring Cloud Gateway MVC's URI handling can decode %26 to & in query strings,
  // which breaks parameters containing literal & characters (e.g., "Aggregation & Grouping").
  // This filter reconstructs the URI with the raw query string from the servlet request.
  private fun preserveRawQueryString(request: ServerRequest): ServerRequest {
    val servletRequest: HttpServletRequest = request.servletRequest()
    val rawQuery = servletRequest.queryString ?: return request

    val currentUri = request.uri()
    val correctedUri = URI(
      currentUri.scheme,
      currentUri.authority,
      currentUri.path,
      null, // clear parsed query to use rawSchemeSpecificPart
      null
    ).let {
      // Rebuild with raw query string to preserve percent-encoding
      URI("${it.scheme}://${it.authority}${it.path}?${rawQuery}")
    }

    return ServerRequest.from(request)
      .uri(correctedUri)
      .build()
  }

  private fun path(pattern: String) =
    org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path(pattern)
}
