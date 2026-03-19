package org.projectcontinuum.core.bridge.repository

import org.hibernate.validator.constraints.UUID
import org.projectcontinuum.core.bridge.entity.WorkflowRunEntity
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant

interface WorkflowRunRepository: CrudRepository<WorkflowRunEntity, UUID> {
  @Modifying
  @Query("""
    INSERT INTO workflow_runs (workflow_id, owned_by, status, data, created_at, updated_at)
    VALUES (CAST(:workflowId AS UUID), :ownedBy, :status, CAST(:data AS JSONB), :createdAt, :updatedAt)
    ON CONFLICT (workflow_id) DO UPDATE SET
      status = :status,
      data = CAST(:data AS JSONB),
      updated_at = :updatedAt
  """)
  fun upsert(
    workflowId: String,
    ownedBy: String,
    status: String,
    data: String,
    createdAt: Instant,
    updatedAt: Instant
  ): Int
}