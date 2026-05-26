package org.projectcontinuum.core.api.server.entity.jpa

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URI
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "workflow_runs")
class WorkflowRunEntity(
    @Id
    @Column(name = "workflow_id")
    val workflowId: UUID,

    @Column(name = "workflow_type", nullable = false)
    val workflowType: String,

    @Column(name = "owned_by", nullable = false)
    val ownedBy: String,

    @Column(name = "workflow_uri", nullable = false, columnDefinition = "VARCHAR(2048)")
    @Convert(converter = UriAttributeConverter::class)
    val workflowUri: URI,

    @Column(name = "progress_percentage", nullable = false)
    val progressPercentage: Int = 0,

    @Column(name = "status", nullable = false)
    var status: String = "PENDING",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    var data: JsonNode = JsonNodeFactory.instance.objectNode(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
