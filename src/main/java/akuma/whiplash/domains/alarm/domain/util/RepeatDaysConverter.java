package akuma.whiplash.domains.alarm.domain.util;

import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class RepeatDaysConverter implements AttributeConverter<List<Weekday>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Weekday> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(attribute);  // 예: ["MONDAY", "WEDNESDAY"]
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize repeatDays", e);
        }
    }

    @Override
    public List<Weekday> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Weekday>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not deserialize repeatDays", e);
        }
    }
}
