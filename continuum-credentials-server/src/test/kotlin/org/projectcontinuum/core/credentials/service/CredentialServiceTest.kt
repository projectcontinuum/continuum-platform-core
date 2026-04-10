package org.projectcontinuum.core.credentials.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.projectcontinuum.core.credentials.entity.CredentialEntity
import org.projectcontinuum.core.credentials.exception.CredentialAlreadyExistsException
import org.projectcontinuum.core.credentials.exception.CredentialNotFoundException
import org.projectcontinuum.core.credentials.model.CredentialCreateRequest
import org.projectcontinuum.core.credentials.model.CredentialType
import org.projectcontinuum.core.credentials.model.CredentialUpdateRequest
import org.projectcontinuum.core.credentials.repository.CredentialRepository
import java.time.Instant
import java.util.UUID

class CredentialServiceTest {

  private lateinit var credentialRepository: CredentialRepository
  private lateinit var encryptionService: EncryptionService
  private lateinit var credentialService: CredentialService

  private val userId = "user-1"

  @BeforeEach
  fun setUp() {
    credentialRepository = mock()
    encryptionService = mock()
    credentialService = CredentialService(credentialRepository, encryptionService)

    whenever(encryptionService.encrypt(any())).thenReturn("encrypted-data")
    whenever(encryptionService.decrypt(any())).thenReturn("""{"key":"value"}""")
  }

  private fun sampleEntity(
    name: String = "my-s3",
    userId: String = this.userId
  ): CredentialEntity {
    val now = Instant.now()
    return CredentialEntity(
      credentialId = UUID.randomUUID(),
      userId = userId,
      name = name,
      type = CredentialType.S3.name,
      data = "encrypted-data",
      description = "Test credential",
      createdBy = userId,
      updatedBy = userId,
      createdAt = now,
      updatedAt = now
    )
  }

  @Test
  fun `createCredential saves and returns response`() {
    val request = CredentialCreateRequest(
      name = "my-s3",
      type = CredentialType.S3,
      data = """{"key":"value"}""",
      description = "Test"
    )
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(false)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.createCredential(userId, request)

    assertEquals("my-s3", response.name)
    assertEquals(CredentialType.S3, response.type)
    assertEquals(userId, response.userId)
    verify(encryptionService).encrypt("""{"key":"value"}""")
    verify(credentialRepository).save(any())
  }

  @Test
  fun `createCredential throws when name already exists`() {
    val request = CredentialCreateRequest(
      name = "my-s3",
      type = CredentialType.S3,
      data = """{"key":"value"}"""
    )
    whenever(credentialRepository.existsByUserIdAndName(userId, "my-s3")).thenReturn(true)

    assertThrows(CredentialAlreadyExistsException::class.java) {
      credentialService.createCredential(userId, request)
    }
    verify(credentialRepository, never()).save(any())
  }

  @Test
  fun `getCredential returns decrypted response and updates lastAccessedAt`() {
    val entity = sampleEntity()
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(entity)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.getCredential(userId, "my-s3")

    assertEquals("my-s3", response.name)
    verify(encryptionService).decrypt("encrypted-data")
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
    verify(credentialRepository, times(2)).save(any())
  }

  @Test
  fun `getCredentialsByType filters by type`() {
    val entities = listOf(sampleEntity("s3-cred"))
    whenever(credentialRepository.findAllByUserIdAndType(userId, "S3")).thenReturn(entities)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.getCredentialsByType(userId, CredentialType.S3)

    assertEquals(1, response.size)
    assertEquals(CredentialType.S3, response[0].type)
  }

  @Test
  fun `updateCredential updates fields and re-encrypts data`() {
    val entity = sampleEntity()
    val request = CredentialUpdateRequest(
      data = """{"newKey":"newValue"}""",
      description = "Updated"
    )
    whenever(credentialRepository.findByUserIdAndName(userId, "my-s3")).thenReturn(entity)
    whenever(credentialRepository.save(any())).thenAnswer { it.arguments[0] as CredentialEntity }

    val response = credentialService.updateCredential(userId, "my-s3", request)

    assertNotNull(response)
    verify(encryptionService).encrypt("""{"newKey":"newValue"}""")
    verify(credentialRepository).save(any())
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
  fun `updateCredential throws when not found`() {
    whenever(credentialRepository.findByUserIdAndName(userId, "nonexistent")).thenReturn(null)

    assertThrows(CredentialNotFoundException::class.java) {
      credentialService.updateCredential(userId, "nonexistent", CredentialUpdateRequest())
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
