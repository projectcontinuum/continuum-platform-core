package org.projectcontinuum.core.credentials.repository

import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity

interface CredentialTypeRepository {

  fun save(entity: CredentialTypeEntity): CredentialTypeEntity

  fun findByType(type: String): CredentialTypeEntity?

  fun findAll(): List<CredentialTypeEntity>

  fun deleteByType(type: String)

  fun existsByType(type: String): Boolean
}
