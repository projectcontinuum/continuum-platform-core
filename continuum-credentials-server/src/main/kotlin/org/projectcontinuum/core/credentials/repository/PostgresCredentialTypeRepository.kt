package org.projectcontinuum.core.credentials.repository

import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity

class PostgresCredentialTypeRepository(
  private val springDataRepository: SpringDataCredentialTypeRepository
) : CredentialTypeRepository {

  override fun save(entity: CredentialTypeEntity): CredentialTypeEntity =
    springDataRepository.save(entity)

  override fun findByTypeAndVersion(type: String, version: String): CredentialTypeEntity? =
    springDataRepository.findByTypeAndVersion(type, version)

  override fun findAllByType(type: String): List<CredentialTypeEntity> =
    springDataRepository.findAllByType(type)

  override fun findAll(): List<CredentialTypeEntity> =
    springDataRepository.findAll().toList()

  override fun deleteByTypeAndVersion(type: String, version: String) =
    springDataRepository.deleteByTypeAndVersion(type, version)

  override fun existsByType(type: String): Boolean =
    springDataRepository.existsByType(type)

  override fun existsByTypeAndVersion(type: String, version: String): Boolean =
    springDataRepository.existsByTypeAndVersion(type, version)
}
