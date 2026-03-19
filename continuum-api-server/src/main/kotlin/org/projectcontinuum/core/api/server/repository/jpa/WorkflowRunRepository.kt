package org.projectcontinuum.core.api.server.repository.jpa

import org.projectcontinuum.core.api.server.entity.jpa.WorkflowRunEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface WorkflowRunRepository : JpaRepository<WorkflowRunEntity, UUID>, JpaSpecificationExecutor<WorkflowRunEntity> {
    fun findByOwnedBy(ownedBy: String): List<WorkflowRunEntity>
}
