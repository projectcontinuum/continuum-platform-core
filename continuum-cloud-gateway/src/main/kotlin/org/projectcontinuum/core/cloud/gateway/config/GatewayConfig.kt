package org.projectcontinuum.core.cloud.gateway.config

import org.springframework.context.annotation.Configuration

// Spring Cloud Gateway Server MVC configuration.
//
// The dynamic workbench proxy routing (/workbench/{instanceName}/open)
// is handled by WorkbenchProxyController and WorkbenchWebSocketProxyHandler because
// the target URL must be resolved dynamically from the database at request time.
//
// This configuration class can be extended with static gateway routes if needed
// in the future (e.g., for routing to other known backend services).
@Configuration
class GatewayConfig
