package org.projectcontinuum.core.credentials.repository

import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity

interface CredentialTypeRepository {

  fun save(entity: CredentialTypeEntity): CredentialTypeEntity

  fun findByTypeAndVersion(type: String, version: String): CredentialTypeEntity?

  fun findAllByType(type: String): List<CredentialTypeEntity>

  fun findAll(): List<CredentialTypeEntity>

  fun deleteByTypeAndVersion(type: String, version: String)

  fun existsByType(type: String): Boolean

  fun existsByTypeAndVersion(type: String, version: String): Boolean
}
