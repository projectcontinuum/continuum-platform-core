package org.projectcontinuum.core.credentials.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.projectcontinuum.core.credentials.exception.CredentialAlreadyExistsException
import org.projectcontinuum.core.credentials.exception.CredentialNotFoundException
import org.projectcontinuum.core.credentials.exception.CredentialTypeNotFoundException
import org.projectcontinuum.core.credentials.model.CredentialCreateRequest
import org.projectcontinuum.core.credentials.model.CredentialResponse
import org.projectcontinuum.core.credentials.model.CredentialUpdateRequest
import org.projectcontinuum.core.credentials.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(CredentialController::class)
class CredentialControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @MockitoBean
  private lateinit var credentialService: CredentialService

  private val userId = "user-1"
  private val now = Instant.parse("2024-01-01T00:00:00Z")

  private fun sampleResponse(name: String = "my-s3") = CredentialResponse(
    userId = userId,
    name = name,
    type = "s3",
    data = mapOf("accessKey" to "AKIAIOSFODNN7", "secretKey" to "wJalrXUtnFEMI"),
    description = "Test",
    createdBy = userId,
    updatedBy = userId,
    createdAt = now,
    updatedAt = now,
    lastAccessedAt = now
  )

  @Test
  fun `POST creates credential and returns 201`() {
    val request = CredentialCreateRequest(
      name = "my-s3",
      type = "s3",
      data = mapOf("accessKey" to "AKIAIOSFODNN7", "secretKey" to "wJalrXUtnFEMI"),
      description = "Test"
    )
    whenever(credentialService.createCredential(eq(userId), any())).thenReturn(sampleResponse())

    mockMvc.perform(
      post("/api/v1/credentials")
        .header("x-continuum-user-id", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isCreated)
      .andExpect(jsonPath("$.name").value("my-s3"))
      .andExpect(jsonPath("$.type").value("s3"))
      .andExpect(jsonPath("$.data.accessKey").value("AKIAIOSFODNN7"))
  }

  @Test
  fun `POST with invalid type returns 404 when type not found`() {
    val request = CredentialCreateRequest(
      name = "my-cred",
      type = "unknown",
      data = mapOf("key" to "value")
    )
    whenever(credentialService.createCredential(eq(userId), any()))
      .thenThrow(CredentialTypeNotFoundException("Credential type 'unknown' does not exist"))

    mockMvc.perform(
      post("/api/v1/credentials")
        .header("x-continuum-user-id", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isNotFound)
      .andExpect(jsonPath("$.error").value("Credential type 'unknown' does not exist"))
  }

  @Test
  fun `POST with empty data map returns 400`() {
    val request = mapOf(
      "name" to "my-s3",
      "type" to "s3",
      "data" to emptyMap<String, String>()
    )

    mockMvc.perform(
      post("/api/v1/credentials")
        .header("x-continuum-user-id", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `POST with invalid name returns 400`() {
    val request = mapOf(
      "name" to "invalid name",
      "type" to "s3",
      "data" to mapOf("key" to "value")
    )

    mockMvc.perform(
      post("/api/v1/credentials")
        .header("x-continuum-user-id", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `POST with duplicate name returns 409`() {
    val request = CredentialCreateRequest(name = "my-s3", type = "s3", data = mapOf("key" to "value"))
    whenever(credentialService.createCredential(eq(userId), any()))
      .thenThrow(CredentialAlreadyExistsException("Credential 'my-s3' already exists"))

    mockMvc.perform(
      post("/api/v1/credentials")
        .header("x-continuum-user-id", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isConflict)
  }

  @Test
  fun `GET list returns all credentials for user`() {
    whenever(credentialService.listCredentials(userId))
      .thenReturn(listOf(sampleResponse("cred-1"), sampleResponse("cred-2")))

    mockMvc.perform(
      get("/api/v1/credentials").header("x-continuum-user-id", userId)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(2))
  }

  @Test
  fun `GET by name returns credential`() {
    whenever(credentialService.getCredential(userId, "my-s3")).thenReturn(sampleResponse())

    mockMvc.perform(
      get("/api/v1/credentials/my-s3").header("x-continuum-user-id", userId)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.name").value("my-s3"))
      .andExpect(jsonPath("$.type").value("s3"))
  }

  @Test
  fun `GET by name returns 404 when not found`() {
    whenever(credentialService.getCredential(userId, "nonexistent"))
      .thenThrow(CredentialNotFoundException("Not found"))

    mockMvc.perform(
      get("/api/v1/credentials/nonexistent").header("x-continuum-user-id", userId)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `GET by type returns filtered credentials`() {
    whenever(credentialService.getCredentialsByType(userId, "s3"))
      .thenReturn(listOf(sampleResponse()))

    mockMvc.perform(
      get("/api/v1/credentials/type/s3").header("x-continuum-user-id", userId)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].type").value("s3"))
  }

  @Test
  fun `PUT updates credential and returns 200`() {
    val request = CredentialUpdateRequest(data = mapOf("newKey" to "newValue"))
    whenever(credentialService.updateCredential(eq(userId), eq("my-s3"), any()))
      .thenReturn(sampleResponse())

    mockMvc.perform(
      put("/api/v1/credentials/my-s3")
        .header("x-continuum-user-id", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isOk)
  }

  @Test
  fun `DELETE returns 204`() {
    doNothing().whenever(credentialService).deleteCredential(userId, "my-s3")

    mockMvc.perform(
      delete("/api/v1/credentials/my-s3").header("x-continuum-user-id", userId)
    )
      .andExpect(status().isNoContent)
  }

  @Test
  fun `DELETE returns 404 when not found`() {
    doThrow(CredentialNotFoundException("Not found"))
      .whenever(credentialService).deleteCredential(userId, "nonexistent")

    mockMvc.perform(
      delete("/api/v1/credentials/nonexistent").header("x-continuum-user-id", userId)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `defaults to anonymous user when header is missing`() {
    whenever(credentialService.listCredentials("anonymous")).thenReturn(emptyList())

    mockMvc.perform(get("/api/v1/credentials"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))
  }
}
