package org.projectcontinuum.core.cloud.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse

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
      .build()

  @Bean
  fun clusterManagerRoute(): RouterFunction<ServerResponse> =
    route("cluster-manager")
      .route(path("/cluster-manager/**"), http(clusterManagerUrl))
      .before(stripPrefix(1))
      .build()

  private fun path(pattern: String) =
    org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path(pattern)
}
