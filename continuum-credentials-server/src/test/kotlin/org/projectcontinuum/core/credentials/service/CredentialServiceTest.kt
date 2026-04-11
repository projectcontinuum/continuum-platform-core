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
    whenever(credentialTypeRepository.existsByType(any())).thenReturn(true)
  }

  private fun sampleEntity(name: String = "my-s3"): CredentialEntity {
    val now = Instant.now()
    val encryptedData = objectMapper.writeValueAsString(
      mapOf("accessKey" to "${cipherPrefix}enc(AKIAIOSFODNN7)", "secretKey" to "${cipherPrefix}enc(wJalrXUtnFEMI)")
    )
    return CredentialEntity(
      credentialId = UUID.randomUUID(),
      userId = userId,
      name = name,
      type = "s3",
      data = JsonValue(encryptedData),
      description = "Test credential",
      createdBy = userId,
      updatedBy = userId,
      createdAt = now,
      updatedAt = now
    )
  }

  @Test
  fun `createCredential encrypts values with cipher prefix`() {
    val request = CredentialCreateRequest(
      name = "my-s3", type = "s3",
      data = mapOf("accessKey" to "AKIAIOSFODNN7", "secretKey" to "wJalrXUtnFEMI")
    )
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(false)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.createCredential(userId, request)

    assertEquals("AKIAIOSFODNN7", response.data["accessKey"])
    assertEquals("wJalrXUtnFEMI", response.data["secretKey"])
    verify(credentialTypeRepository).existsByType("s3")
  }

  @Test
  fun `createCredential throws when type does not exist`() {
    whenever(credentialTypeRepository.existsByType("unknown")).thenReturn(false)

    assertThrows(CredentialTypeNotFoundException::class.java) {
      credentialService.createCredential(userId, CredentialCreateRequest(name = "x", type = "unknown", data = mapOf("k" to "v")))
    }
  }

  @Test
  fun `createCredential throws when name already exists`() {
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(true)

    assertThrows(CredentialAlreadyExistsException::class.java) {
      credentialService.createCredential(userId, CredentialCreateRequest(name = "my-s3", type = "s3", data = mapOf("k" to "v")))
    }
  }

  @Test
  fun `getCredential strips cipher prefix and decrypts`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(sampleEntity())
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.getCredential(userId, "my-s3")

    assertEquals("AKIAIOSFODNN7", response.data["accessKey"])
    verify(credentialRepository).save(argThat { lastAccessedAt != null })
  }

  @Test
  fun `getCredential throws when not found`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "x")).thenReturn(null)

    assertThrows(CredentialNotFoundException::class.java) {
      credentialService.getCredential(userId, "x")
    }
  }

  @Test
  fun `listCredentials returns all for user`() {
    whenever(credentialRepository.findAllByUserId(userId)).thenReturn(listOf(sampleEntity("a"), sampleEntity("b")))
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    assertEquals(2, credentialService.listCredentials(userId).size)
  }

  @Test
  fun `updateCredential validates type when changing`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(sampleEntity())
    whenever(credentialTypeRepository.existsByType("unknown")).thenReturn(false)

    assertThrows(CredentialTypeNotFoundException::class.java) {
      credentialService.updateCredential(userId, "my-s3", CredentialUpdateRequest(type = "unknown"))
    }
  }

  @Test
  fun `deleteCredential deletes by userId and name`() {
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(true)
    credentialService.deleteCredential(userId, "my-s3")
    verify(credentialRepository).deleteByUserIdAndName(userId, "my-s3")
  }

  @Test
  fun `deleteCredential throws when not found`() {
    whenever(credentialRepository.existsByUserIdAndName(userId, "x")).thenReturn(false)

    assertThrows(CredentialNotFoundException::class.java) {
      credentialService.deleteCredential(userId, "x")
    }
  }
}
