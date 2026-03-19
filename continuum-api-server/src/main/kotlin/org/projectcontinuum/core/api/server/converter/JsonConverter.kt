package jarvis.core.audit.converter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.io.IOException

@Converter(autoApply = true)
class JsonConverter : AttributeConverter<MutableMap<CharSequence?, CharSequence?>?, String?> {
  private val objectMapper = ObjectMapper()

  override fun convertToDatabaseColumn(attribute: MutableMap<CharSequence?, CharSequence?>?): String? {
    if (attribute == null) {
      return null
    }
    try {
      return objectMapper.writeValueAsString(attribute)
    } catch (e: JsonProcessingException) {
      throw RuntimeException(
        "Failed to convert map to json string",
        e
      )
    }
  }

  override fun convertToEntityAttribute(dbData: String?): MutableMap<CharSequence?, CharSequence?>? {
    try {
      if (dbData != null) {
        return objectMapper.readValue<MutableMap<CharSequence?, CharSequence?>?>(
          dbData,
          object : TypeReference<MutableMap<CharSequence?, CharSequence?>?>() {
          }
        )
      } else {
        return null
      }
    } catch (e: IOException) {
      throw RuntimeException(
        "Failed to convert json string to map",
        e
      )
    }
  }
}