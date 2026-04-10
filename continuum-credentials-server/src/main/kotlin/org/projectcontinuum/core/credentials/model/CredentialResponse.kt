package org.projectcontinuum.core.credentials.model

import java.time.Instant

data class CredentialResponse(
  val userId: String,
  val name: String,
  val type: CredentialType,
  val data: String,
  val description: String?,
  val createdBy: String,
  val updatedBy: String,
  val createdAt: Instant,
  val updatedAt: Instant,
  val lastAccessedAt: Instant?
)
