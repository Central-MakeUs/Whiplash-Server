package akuma.whiplash.global.util.date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * JsonDeserializer that delegates parsing to {@link DateUtil#parseLocalTimeWith24HourSupport(String)}
 * to accept "24:xx" values and normalize them to "00:xx".
 */
public class LocalTime24HourDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
        String value = p.getValueAsString();
        try {
            return DateUtil.parseLocalTimeWith24HourSupport(value);
        } catch (DateTimeParseException e) {
            throw JsonMappingException.from(p, "Invalid time format", e);
        }
    }
}