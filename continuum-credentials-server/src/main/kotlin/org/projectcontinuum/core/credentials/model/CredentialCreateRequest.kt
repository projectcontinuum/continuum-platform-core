package org.projectcontinuum.core.credentials.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CredentialCreateRequest(
  @field:NotBlank(message = "Name is required")
  @field:Pattern(
    regexp = "^[a-zA-Z0-9_-]+$",
    message = "Name must contain only alphanumeric characters, hyphens, and underscores"
  )
  @field:Size(max = 255, message = "Name must be at most 255 characters")
  val name: String,

  @field:NotNull(message = "Type is required")
  val type: CredentialType,

  @field:NotBlank(message = "Data is required")
  val data: String,

  @field:Size(max = 1000, message = "Description must be at most 1000 characters")
  val description: String? = null
)
