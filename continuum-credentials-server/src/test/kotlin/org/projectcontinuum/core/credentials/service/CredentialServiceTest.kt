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

  private fun sampleEntity(
    name: String = "my-s3",
    userId: String = this.userId
  ): CredentialEntity {
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
      name = "my-s3",
      type = "s3",
      data = mapOf("accessKey" to "AKIAIOSFODNN7", "secretKey" to "wJalrXUtnFEMI"),
      description = "Test"
    )
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(false)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.createCredential(userId, request)

    verify(credentialRepository).save(argThat {
      data.value.contains("${cipherPrefix}enc(AKIAIOSFODNN7)") && data.value.contains("${cipherPrefix}enc(wJalrXUtnFEMI)")
    })

    assertEquals("AKIAIOSFODNN7", response.data["accessKey"])
    assertEquals("wJalrXUtnFEMI", response.data["secretKey"])
    assertEquals("s3", response.type)
  }

  @Test
  fun `createCredential throws when type does not exist`() {
    whenever(credentialTypeRepository.existsByType("unknown")).thenReturn(false)

    val request = CredentialCreateRequest(
      name = "my-cred",
      type = "unknown",
      data = mapOf("key" to "value")
    )

    assertThrows(CredentialTypeNotFoundException::class.java) {
      credentialService.createCredential(userId, request)
    }
    verify(credentialRepository, never()).save(any())
  }

  @Test
  fun `createCredential throws when name already exists`() {
    val request = CredentialCreateRequest(
      name = "my-s3",
      type = "s3",
      data = mapOf("key" to "value")
    )
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(true)

    assertThrows(CredentialAlreadyExistsException::class.java) {
      credentialService.createCredential(userId, request)
    }
  }

  @Test
  fun `getCredential strips cipher prefix and decrypts values`() {
    val entity = sampleEntity()
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(entity)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.getCredential(userId, "my-s3")

    assertEquals("AKIAIOSFODNN7", response.data["accessKey"])
    assertEquals("wJalrXUtnFEMI", response.data["secretKey"])
    verify(credentialRepository).save(argThat { lastAccessedAt != null })
  }

  @Test
  fun `getCredential throws when not found`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "nonexistent")).thenReturn(null)

    assertThrows(CredentialNotFoundException::class.java) {
      credentialService.getCredential(userId, "nonexistent")
    }
  }

  @Test
  fun `listCredentials returns all credentials for user`() {
    val entities = listOf(sampleEntity("cred-1"), sampleEntity("cred-2"))
    whenever(credentialRepository.findAllByUserId(userId)).thenReturn(entities)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.listCredentials(userId)

    assertEquals(2, response.size)
  }

  @Test
  fun `getCredentialsByType filters by type`() {
    val entities = listOf(sampleEntity("s3-cred"))
    whenever(credentialRepository.findAllByUserIdAndType(userId, "s3")).thenReturn(entities)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.getCredentialsByType(userId, "s3")

    assertEquals(1, response.size)
    assertEquals("s3", response[0].type)
  }

  @Test
  fun `updateCredential validates type when changing`() {
    val entity = sampleEntity()
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(entity)
    whenever(credentialTypeRepository.existsByType("unknown")).thenReturn(false)

    val request = CredentialUpdateRequest(type = "unknown")

    assertThrows(CredentialTypeNotFoundException::class.java) {
      credentialService.updateCredential(userId, "my-s3", request)
    }
  }

  @Test
  fun `updateCredential re-encrypts new map data with cipher prefix`() {
    val entity = sampleEntity()
    val request = CredentialUpdateRequest(data = mapOf("newKey" to "newValue"))
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(entity)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    credentialService.updateCredential(userId, "my-s3", request)

    verify(credentialRepository).save(argThat {
      data.value.contains("${cipherPrefix}enc(newValue)")
    })
  }

  @Test
  fun `updateCredential checks name uniqueness when renaming`() {
    val entity = sampleEntity()
    val request = CredentialUpdateRequest(name = "new-name")
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(entity)
    whenever(credentialRepository.existsByUserIdAndName(userId, "new-name")).thenReturn(true)

    assertThrows(CredentialAlreadyExistsException::class.java) {
      credentialService.updateCredential(userId, "my-s3", request)
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
    whenever(credentialRepository.existsByUserIdAndName(userId, "nonexistent")).thenReturn(false)

    assertThrows(CredentialNotFoundException::class.java) {
      credentialService.deleteCredential(userId, "nonexistent")
    }
  }
}
