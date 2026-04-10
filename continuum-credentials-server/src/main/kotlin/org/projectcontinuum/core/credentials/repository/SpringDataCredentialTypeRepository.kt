package org.projectcontinuum.core.credentials.repository

import org.projectcontinuum.core.credentials.entity.CredentialTypeEntity
import org.springframework.data.repository.CrudRepository

interface SpringDataCredentialTypeRepository : CrudRepository<CredentialTypeEntity, String>
