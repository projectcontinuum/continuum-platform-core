package org.projectcontinuum.core.api.server.entity.jpa

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "workflow_runs")
class WorkflowRunEntity(
    @Id
    @Column(name = "workflow_id")
    val workflowId: UUID,

    @Column(name = "owned_by", nullable = false)
    val ownedBy: String,

    @Column(name = "status", nullable = false)
    var status: String = "PENDING",

    @Type(JsonType::class)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    var data: Map<String, Any> = emptyMap(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
