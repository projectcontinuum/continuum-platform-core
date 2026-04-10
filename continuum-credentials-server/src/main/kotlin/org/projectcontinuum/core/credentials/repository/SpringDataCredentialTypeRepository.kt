package org.projectcontinuum.core.credentials.repository

import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface SpringDataCredentialTypeRepository : CrudRepository<CredentialTypeEntity, UUID> {

  @Query("SELECT * FROM credential_types WHERE type = :type LIMIT 1")
  fun findByType(type: String): CredentialTypeEntity?

  @Modifying
  @Query("DELETE FROM credential_types WHERE type = :type")
  fun deleteByType(type: String)

  @Query("SELECT COUNT(*) > 0 FROM credential_types WHERE type = :type")
  fun existsByType(type: String): Boolean
}
