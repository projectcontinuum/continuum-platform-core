package org.projectcontinuum.core.credentials.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.projectcontinuum.core.credentials.entity.CredentialEntity
import org.projectcontinuum.core.credentials.entity.JsonValue
import org.projectcontinuum.core.credentials.exception.CredentialAlreadyExistsException
import org.projectcontinuum.core.credentials.exception.CredentialNotFoundException
import org.projectcontinuum.core.credentials.exception.CredentialTypeNotFoundException
import org.projectcontinuum.core.credentials.model.CredentialCreateRequest
import org.projectcontinuum.core.credentials.model.CredentialUpdateRequest
import org.projectcontinuum.core.credentials.repository.CredentialRepository
import org.projectcontinuum.core.credentials.repository.CredentialTypeRepository
import java.time.Instant
import java.util.UUID

class CredentialServiceTest {

  private lateinit var credentialRepository: CredentialRepository
  private lateinit var credentialTypeRepository: CredentialTypeRepository
  private lateinit var encryptionService: EncryptionService
  private lateinit var credentialService: CredentialService
  private val objectMapper: ObjectMapper = jacksonObjectMapper()

  private val userId = "user-1"
  private val cipherPrefix = "{cipher}"

  @BeforeEach
  fun setUp() {
    credentialRepository = mock()
    credentialTypeRepository = mock()
    encryptionService = mock()
    credentialService = CredentialService(credentialRepository, credentialTypeRepository, encryptionService, objectMapper)

    whenever(encryptionService.encrypt(any())).thenAnswer { "enc(${it.arguments[0]})" }
    whenever(encryptionService.decrypt(any())).thenAnswer {
      val arg = it.arguments[0] as String
      arg.removePrefix("enc(").removeSuffix(")")
    }
    whenever(credentialTypeRepository.existsByTypeAndVersion(any(), any())).thenReturn(true)
  }

  private fun sampleEntity(name: String = "my-s3"): CredentialEntity {
    val encryptedData = objectMapper.writeValueAsString(
      mapOf("accessKey" to "${cipherPrefix}enc(AKIAIOSFODNN7)", "secretKey" to "${cipherPrefix}enc(wJalrXUtnFEMI)")
    )
    return CredentialEntity(
      credentialId = UUID.randomUUID(),
      userId = userId,
      name = name,
      type = "s3",
      typeVersion = "1.0.0",
      data = JsonValue(encryptedData),
      description = "Test",
      createdBy = userId,
      updatedBy = userId,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  }

  @Test
  fun `createCredential validates type and version together`() {
    val request = CredentialCreateRequest(
      name = "my-s3", type = "s3", typeVersion = "1.0.0",
      data = mapOf("accessKey" to "AKIAIOSFODNN7")
    )
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(false)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.createCredential(userId, request)

    assertEquals("s3", response.type)
    assertEquals("1.0.0", response.typeVersion)
    verify(credentialTypeRepository).existsByTypeAndVersion("s3", "1.0.0")
  }

  @Test
  fun `createCredential throws when type+version does not exist`() {
    whenever(credentialTypeRepository.existsByTypeAndVersion("s3", "9.0.0")).thenReturn(false)

    assertThrows(CredentialTypeNotFoundException::class.java) {
      credentialService.createCredential(userId, CredentialCreateRequest(
        name = "x", type = "s3", typeVersion = "9.0.0", data = mapOf("k" to "v")
      ))
    }
  }

  @Test
  fun `createCredential throws when name already exists`() {
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(true)

    assertThrows(CredentialAlreadyExistsException::class.java) {
      credentialService.createCredential(userId, CredentialCreateRequest(
        name = "my-s3", type = "s3", typeVersion = "1.0.0", data = mapOf("k" to "v")
      ))
    }
  }

  @Test
  fun `getCredential returns typeVersion in response`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(sampleEntity())
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.getCredential(userId, "my-s3")

    assertEquals("1.0.0", response.typeVersion)
    assertEquals("AKIAIOSFODNN7", response.data["accessKey"])
  }

  @Test
  fun `updateCredential validates new type+version`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(sampleEntity())
    whenever(credentialTypeRepository.existsByTypeAndVersion("git", "2.0.0")).thenReturn(false)

    assertThrows(CredentialTypeNotFoundException::class.java) {
      credentialService.updateCredential(userId, "my-s3", CredentialUpdateRequest(type = "git", typeVersion = "2.0.0"))
    }
  }

  @Test
  fun `updateCredential can change typeVersion`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(sampleEntity())
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.updateCredential(userId, "my-s3", CredentialUpdateRequest(typeVersion = "2.0.0"))

    assertEquals("2.0.0", response.typeVersion)
    verify(credentialTypeRepository).existsByTypeAndVersion("s3", "2.0.0")
  }

  @Test
  fun `deleteCredential throws when not found`() {
    whenever(credentialRepository.existsByUserIdAndName(userId, "x")).thenReturn(false)

    assertThrows(CredentialNotFoundException::class.java) {
      credentialService.deleteCredential(userId, "x")
    }
  }
}
