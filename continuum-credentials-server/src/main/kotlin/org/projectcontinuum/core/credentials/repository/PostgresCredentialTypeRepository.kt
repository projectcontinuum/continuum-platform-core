package org.projectcontinuum.core.credentials.repository

import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity

class PostgresCredentialTypeRepository(
  private val springDataRepository: SpringDataCredentialTypeRepository
) : CredentialTypeRepository {

  override fun save(entity: CredentialTypeEntity): CredentialTypeEntity =
    springDataRepository.save(entity)

  override fun findByType(type: String): CredentialTypeEntity? =
    springDataRepository.findById(type).orElse(null)

  override fun findAll(): List<CredentialTypeEntity> =
    springDataRepository.findAll().toList()

  override fun deleteByType(type: String) =
    springDataRepository.deleteById(type)

  override fun existsByType(type: String): Boolean =
    springDataRepository.existsById(type)
}
