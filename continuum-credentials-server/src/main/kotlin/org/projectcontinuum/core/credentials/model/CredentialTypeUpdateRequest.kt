package org.projectcontinuum.core.credentials.model

import jakarta.validation.constraints.Size

data class CredentialTypeUpdateRequest(
  val schema: Map<String, Any?>? = null,
  val uiSchema: Map<String, Any?>? = null,
  @field:Size(max = 50, message = "Version must be at most 50 characters")
  val version: String? = null
)
