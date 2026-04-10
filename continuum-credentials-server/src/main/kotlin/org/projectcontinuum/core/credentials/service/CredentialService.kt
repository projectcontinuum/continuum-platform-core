package org.projectcontinuum.core.credentials.service

import org.projectcontinuum.core.credentials.entity.CredentialEntity
import org.projectcontinuum.core.credentials.exception.CredentialAlreadyExistsException
import org.projectcontinuum.core.credentials.exception.CredentialNotFoundException
import org.projectcontinuum.core.credentials.model.CredentialCreateRequest
import org.projectcontinuum.core.credentials.model.CredentialResponse
import org.projectcontinuum.core.credentials.model.CredentialType
import org.projectcontinuum.core.credentials.model.CredentialUpdateRequest
import org.projectcontinuum.core.credentials.repository.CredentialRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class CredentialService(
  private val credentialRepository: CredentialRepository,
  private val encryptionService: EncryptionService
) {

  private val logger = LoggerFactory.getLogger(CredentialService::class.java)

  fun createCredential(userId: String, request: CredentialCreateRequest): CredentialResponse {
    if (credentialRepository.existsByUserIdAndName(userId, request.name)) {
      throw CredentialAlreadyExistsException(
        "Credential '${request.name}' already exists for user '$userId'"
      )
    }

    val now = Instant.now()
    val entity = CredentialEntity(
      credentialId = UUID.randomUUID(),
      userId = userId,
      name = request.name,
      type = request.type.name,
      data = encryptionService.encrypt(request.data),
      description = request.description,
      createdBy = userId,
      updatedBy = userId,
      createdAt = now,
      updatedAt = now
    )

    val saved = credentialRepository.save(entity)
    logger.info("Created credential '{}' for user '{}'", saved.name, userId)
    return toResponse(saved)
  }

  fun listCredentials(userId: String): List<CredentialResponse> {
    val entities = credentialRepository.findAllByUserId(userId)
    return entities.map { entity ->
      val updated = entity.copy(lastAccessedAt = Instant.now())
      credentialRepository.save(updated)
      toResponse(updated)
    }
  }

  fun getCredential(userId: String, name: String): CredentialResponse {
    val entity = credentialRepository.findByUserIdAndName(userId, name)
      ?: throw CredentialNotFoundException("Credential '$name' not found for user '$userId'")

    val updated = entity.copy(lastAccessedAt = Instant.now())
    credentialRepository.save(updated)
    return toResponse(updated)
  }

  fun getCredentialsByType(userId: String, type: CredentialType): List<CredentialResponse> {
    val entities = credentialRepository.findAllByUserIdAndType(userId, type.name)
    return entities.map { entity ->
      val updated = entity.copy(lastAccessedAt = Instant.now())
      credentialRepository.save(updated)
      toResponse(updated)
    }
  }

  fun updateCredential(userId: String, name: String, request: CredentialUpdateRequest): CredentialResponse {
    val entity = credentialRepository.findByUserIdAndName(userId, name)
      ?: throw CredentialNotFoundException("Credential '$name' not found for user '$userId'")

    if (request.name != null && request.name != entity.name) {
      if (credentialRepository.existsByUserIdAndName(userId, request.name)) {
        throw CredentialAlreadyExistsException(
          "Credential '${request.name}' already exists for user '$userId'"
        )
      }
    }

    val updatedEntity = entity.copy(
      name = request.name ?: entity.name,
      type = request.type?.name ?: entity.type,
      data = if (request.data != null) encryptionService.encrypt(request.data) else entity.data,
      description = request.description ?: entity.description,
      updatedBy = userId,
      updatedAt = Instant.now()
    )

    val saved = credentialRepository.save(updatedEntity)
    logger.info("Updated credential '{}' for user '{}'", saved.name, userId)
    return toResponse(saved)
  }

  fun deleteCredential(userId: String, name: String) {
    if (!credentialRepository.existsByUserIdAndName(userId, name)) {
      throw CredentialNotFoundException("Credential '$name' not found for user '$userId'")
    }
    credentialRepository.deleteByUserIdAndName(userId, name)
    logger.info("Deleted credential '{}' for user '{}'", name, userId)
  }

  private fun toResponse(entity: CredentialEntity): CredentialResponse {
    return CredentialResponse(
      userId = entity.userId,
      name = entity.name,
      type = CredentialType.valueOf(entity.type),
      data = encryptionService.decrypt(entity.data),
      description = entity.description,
      createdBy = entity.createdBy,
      updatedBy = entity.updatedBy,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
      lastAccessedAt = entity.lastAccessedAt
    )
  }
}
