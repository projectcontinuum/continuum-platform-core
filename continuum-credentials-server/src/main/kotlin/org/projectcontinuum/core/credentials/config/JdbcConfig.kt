package org.projectcontinuum.core.credentials.config

import org.postgresql.util.PGobject
import org.projectcontinuum.core.credentials.entity.JsonValue
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration

@Configuration
class JdbcConfig : AbstractJdbcConfiguration() {

  override fun userConverters(): List<Any> {
    return listOf(PGobjectToJsonValueConverter(), JsonValueToPGobjectConverter())
  }

  @ReadingConverter
  class PGobjectToJsonValueConverter : Converter<PGobject, JsonValue> {
    override fun convert(source: PGobject): JsonValue {
      return JsonValue(source.value ?: "{}")
    }
  }

  @WritingConverter
  class JsonValueToPGobjectConverter : Converter<JsonValue, PGobject> {
    override fun convert(source: JsonValue): PGobject {
      val jsonb = PGobject()
      jsonb.type = "jsonb"
      jsonb.value = source.value
      return jsonb
    }
  }
}
