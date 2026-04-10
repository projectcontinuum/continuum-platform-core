package org.projectcontinuum.core.credentials.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("credential_types")
data class CredentialTypeEntity(
  @Id
  @Column("type")
  val type: String,
  @Column("schema")
  val schema: JsonValue = JsonValue("{}"),
  @Column("ui_schema")
  val uiSchema: JsonValue = JsonValue("{}"),
  @Column("created_at")
  val createdAt: Instant = Instant.now(),
  @Column("updated_at")
  val updatedAt: Instant = Instant.now(),
  @Transient
  val isNewEntity: Boolean = true
) : Persistable<String> {
  override fun getId(): String = type
  override fun isNew(): Boolean = isNewEntity
}
