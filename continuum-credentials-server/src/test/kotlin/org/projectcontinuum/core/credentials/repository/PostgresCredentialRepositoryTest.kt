package org.projectcontinuum.core.credentials.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectcontinuum.core.credentials.config.H2JdbcConfig
import org.projectcontinuum.core.credentials.entity.CredentialEntity
import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity
import org.projectcontinuum.core.credentials.entity.JsonValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID

@SpringBootTest
@Import(H2JdbcConfig::class)
class PostgresCredentialRepositoryTest {

  @Autowired
  private lateinit var credentialRepository: CredentialRepository

  @Autowired
  private lateinit var credentialTypeRepository: CredentialTypeRepository

  @Autowired
  private lateinit var springDataCredentialRepository: SpringDataCredentialRepository

  @Autowired
  private lateinit var springDataCredentialTypeRepository: SpringDataCredentialTypeRepository

  @BeforeEach
  fun setUp() {
    springDataCredentialRepository.deleteAll()
    springDataCredentialTypeRepository.deleteAll()
    credentialTypeRepository.save(CredentialTypeEntity(credentialTypeId = UUID.randomUUID(), type = "s3", credentialTypeVersion = "1.0.0"))
    credentialTypeRepository.save(CredentialTypeEntity(credentialTypeId = UUID.randomUUID(), type = "s3", credentialTypeVersion = "2.0.0"))
    credentialTypeRepository.save(CredentialTypeEntity(credentialTypeId = UUID.randomUUID(), type = "git", credentialTypeVersion = "1.0.0"))
  }

  private fun createEntity(
    userId: String = "user-1",
    name: String = "test-cred",
    type: String = "s3"
  ): CredentialEntity {
    val now = Instant.now()
    return CredentialEntity(
      credentialId = UUID.randomUUID(),
      userId = userId,
      name = name,
      type = type,
      data = JsonValue("""{"accessKey":"enc-key","secretKey":"enc-secret"}"""),
      description = "Test credential",
      createdBy = userId,
      updatedBy = userId,
      createdAt = now,
      updatedAt = now
    )
  }

  @Test
  fun `save and findByUserIdAndName`() {
    credentialRepository.save(createEntity())

    val found = credentialRepository.findByUserIdAndName("user-1", "test-cred")

    assertNotNull(found)
    assertEquals("test-cred", found!!.name)
    assertEquals("s3", found.type)
  }

  @Test
  fun `findAllByUserId returns all user credentials`() {
    credentialRepository.save(createEntity(name = "cred-1"))
    credentialRepository.save(createEntity(name = "cred-2"))
    credentialRepository.save(createEntity(userId = "user-2", name = "other-cred"))

    assertEquals(2, credentialRepository.findAllByUserId("user-1").size)
  }

  @Test
  fun `findAllByUserIdAndType filters by type`() {
    credentialRepository.save(createEntity(name = "s3-cred", type = "s3"))
    credentialRepository.save(createEntity(name = "git-cred", type = "git"))

    val results = credentialRepository.findAllByUserIdAndType("user-1", "s3")
    assertEquals(1, results.size)
    assertEquals("s3-cred", results[0].name)
  }

  @Test
  fun `different users can have same credential name`() {
    credentialRepository.save(createEntity(userId = "user-1", name = "shared"))
    credentialRepository.save(createEntity(userId = "user-2", name = "shared"))

    assertNotNull(credentialRepository.findByUserIdAndName("user-1", "shared"))
    assertNotNull(credentialRepository.findByUserIdAndName("user-2", "shared"))
  }

  @Test
  fun `credential type can have multiple versions`() {
    val s3Versions = credentialTypeRepository.findAllByType("s3")
    assertEquals(2, s3Versions.size)

    assertNotNull(credentialTypeRepository.findByTypeAndVersion("s3", "1.0.0"))
    assertNotNull(credentialTypeRepository.findByTypeAndVersion("s3", "2.0.0"))
    assertNull(credentialTypeRepository.findByTypeAndVersion("s3", "3.0.0"))
  }

  @Test
  fun `existsByType returns true when any version exists`() {
    assertTrue(credentialTypeRepository.existsByType("s3"))
    assertFalse(credentialTypeRepository.existsByType("nonexistent"))
  }

  @Test
  fun `existsByTypeAndVersion checks specific version`() {
    assertTrue(credentialTypeRepository.existsByTypeAndVersion("s3", "1.0.0"))
    assertFalse(credentialTypeRepository.existsByTypeAndVersion("s3", "3.0.0"))
  }

  @Test
  fun `save updates existing entity`() {
    val entity = credentialRepository.save(createEntity())
    credentialRepository.save(entity.copy(description = "Updated", updatedAt = Instant.now()))

    assertEquals("Updated", credentialRepository.findByUserIdAndName("user-1", "test-cred")!!.description)
  }
}
