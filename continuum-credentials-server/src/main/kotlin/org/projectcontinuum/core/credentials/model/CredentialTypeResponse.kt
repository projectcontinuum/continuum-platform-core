package org.projectcontinuum.core.credentials.model

import java.time.Instant

data class CredentialTypeResponse(
  val type: String,
  val schema: Map<String, Any?>,
  val uiSchema: Map<String, Any?>,
  val createdAt: Instant,
  val updatedAt: Instant
)
