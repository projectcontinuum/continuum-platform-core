package org.projectcontinuum.core.credentials.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity
import org.projectcontinuum.core.credentials.entity.JsonValue
import org.projectcontinuum.core.credentials.exception.CredentialAlreadyExistsException
import org.projectcontinuum.core.credentials.exception.CredentialTypeNotFoundException
import org.projectcontinuum.core.credentials.model.CredentialTypeCreateRequest
import org.projectcontinuum.core.credentials.model.CredentialTypeResponse
import org.projectcontinuum.core.credentials.model.CredentialTypeUpdateRequest
import org.projectcontinuum.core.credentials.repository.CredentialTypeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CredentialTypeService(
  private val credentialTypeRepository: CredentialTypeRepository,
  private val objectMapper: ObjectMapper
) {

  private val logger = LoggerFactory.getLogger(CredentialTypeService::class.java)
  private val mapTypeRef = object : TypeReference<Map<String, Any?>>() {}

  fun createType(request: CredentialTypeCreateRequest): CredentialTypeResponse {
    if (credentialTypeRepository.existsByType(request.type)) {
      throw CredentialAlreadyExistsException("Credential type '${request.type}' already exists")
    }

    val now = Instant.now()
    val entity = CredentialTypeEntity(
      type = request.type,
      schema = JsonValue(objectMapper.writeValueAsString(request.schema)),
      uiSchema = JsonValue(objectMapper.writeValueAsString(request.uiSchema)),
      createdAt = now,
      updatedAt = now,
      isNewEntity = true
    )

    val saved = credentialTypeRepository.save(entity)
    logger.info("Created credential type '{}'", saved.type)
    return toResponse(saved)
  }

  fun getType(type: String): CredentialTypeResponse {
    val entity = credentialTypeRepository.findByType(type)
      ?: throw CredentialTypeNotFoundException("Credential type '$type' not found")
    return toResponse(entity)
  }

  fun listTypes(): List<CredentialTypeResponse> {
    return credentialTypeRepository.findAll().map { toResponse(it) }
  }

  fun updateType(type: String, request: CredentialTypeUpdateRequest): CredentialTypeResponse {
    val entity = credentialTypeRepository.findByType(type)
      ?: throw CredentialTypeNotFoundException("Credential type '$type' not found")

    val updatedEntity = entity.copy(
      schema = if (request.schema != null) JsonValue(objectMapper.writeValueAsString(request.schema)) else entity.schema,
      uiSchema = if (request.uiSchema != null) JsonValue(objectMapper.writeValueAsString(request.uiSchema)) else entity.uiSchema,
      updatedAt = Instant.now(),
      isNewEntity = false
    )

    val saved = credentialTypeRepository.save(updatedEntity)
    logger.info("Updated credential type '{}'", saved.type)
    return toResponse(saved)
  }

  fun deleteType(type: String) {
    if (!credentialTypeRepository.existsByType(type)) {
      throw CredentialTypeNotFoundException("Credential type '$type' not found")
    }
    credentialTypeRepository.deleteByType(type)
    logger.info("Deleted credential type '{}'", type)
  }

  private fun toResponse(entity: CredentialTypeEntity): CredentialTypeResponse {
    return CredentialTypeResponse(
      type = entity.type,
      schema = objectMapper.readValue(entity.schema.value, mapTypeRef),
      uiSchema = objectMapper.readValue(entity.uiSchema.value, mapTypeRef),
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )
  }
}
