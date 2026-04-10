package org.projectcontinuum.core.credentials.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectcontinuum.core.credentials.entity.CredentialEntity
import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity
import org.projectcontinuum.core.credentials.entity.JsonValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID

@SpringBootTest
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
    credentialTypeRepository.save(CredentialTypeEntity(type = "s3"))
    credentialTypeRepository.save(CredentialTypeEntity(type = "git"))
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
    val entity = createEntity()
    credentialRepository.save(entity)

    val found = credentialRepository.findByUserIdAndName("user-1", "test-cred")

    assertNotNull(found)
    assertEquals("test-cred", found!!.name)
    assertEquals("user-1", found.userId)
    assertEquals("s3", found.type)
  }

  @Test
  fun `saved data is preserved as JSON string in JsonValue`() {
    val entity = createEntity()
    credentialRepository.save(entity)

    val found = credentialRepository.findByUserIdAndName("user-1", "test-cred")

    assertNotNull(found)
    assertTrue(found!!.data.value.contains("accessKey"))
    assertTrue(found.data.value.contains("enc-key"))
  }

  @Test
  fun `findByUserIdAndName returns null when not found`() {
    val found = credentialRepository.findByUserIdAndName("user-1", "nonexistent")
    assertNull(found)
  }

  @Test
  fun `findAllByUserId returns all user credentials`() {
    credentialRepository.save(createEntity(name = "cred-1"))
    credentialRepository.save(createEntity(name = "cred-2"))
    credentialRepository.save(createEntity(userId = "user-2", name = "other-cred"))

    val results = credentialRepository.findAllByUserId("user-1")

    assertEquals(2, results.size)
    assertTrue(results.all { it.userId == "user-1" })
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
  fun `existsByUserIdAndName returns true when exists`() {
    credentialRepository.save(createEntity())
    assertTrue(credentialRepository.existsByUserIdAndName("user-1", "test-cred"))
  }

  @Test
  fun `existsByUserIdAndName returns false when not exists`() {
    assertFalse(credentialRepository.existsByUserIdAndName("user-1", "nonexistent"))
  }

  @Test
  fun `deleteByUserIdAndName removes the credential`() {
    credentialRepository.save(createEntity())
    credentialRepository.deleteByUserIdAndName("user-1", "test-cred")
    assertNull(credentialRepository.findByUserIdAndName("user-1", "test-cred"))
  }

  @Test
  fun `different users can have same credential name`() {
    credentialRepository.save(createEntity(userId = "user-1", name = "shared-name"))
    credentialRepository.save(createEntity(userId = "user-2", name = "shared-name"))

    val user1Cred = credentialRepository.findByUserIdAndName("user-1", "shared-name")
    val user2Cred = credentialRepository.findByUserIdAndName("user-2", "shared-name")

    assertNotNull(user1Cred)
    assertNotNull(user2Cred)
    assertEquals("user-1", user1Cred!!.userId)
    assertEquals("user-2", user2Cred!!.userId)
  }

  @Test
  fun `save updates existing entity`() {
    val entity = credentialRepository.save(createEntity())
    val updated = entity.copy(description = "Updated description", updatedAt = Instant.now())
    credentialRepository.save(updated)

    val found = credentialRepository.findByUserIdAndName("user-1", "test-cred")
    assertEquals("Updated description", found!!.description)
  }
}
