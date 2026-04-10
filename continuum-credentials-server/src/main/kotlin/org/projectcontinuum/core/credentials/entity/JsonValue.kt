package org.projectcontinuum.core.credentials.entity

/**
 * Wrapper type for JSONB column values. Used to distinguish JSONB fields from
 * plain String fields so that Spring Data JDBC custom converters only apply
 * to the correct columns.
 */
@JvmInline
value class JsonValue(val value: String) {
  override fun toString(): String = value
}
