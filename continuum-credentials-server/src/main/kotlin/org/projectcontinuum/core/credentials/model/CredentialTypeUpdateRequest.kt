package org.projectcontinuum.core.credentials.model

data class CredentialTypeUpdateRequest(
  val schema: Map<String, Any?>? = null,
  val uiSchema: Map<String, Any?>? = null
)
