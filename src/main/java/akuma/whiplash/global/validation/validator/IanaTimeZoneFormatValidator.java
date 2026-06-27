package akuma.whiplash.global.validation.validator;

import akuma.whiplash.global.validation.annotation.IanaTimeZoneFormat;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DateTimeException;
import java.time.ZoneId;

public class IanaTimeZoneFormatValidator implements ConstraintValidator<IanaTimeZoneFormat, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            ZoneId.of(value);
            return ZoneId.getAvailableZoneIds().contains(value);
        } catch (DateTimeException e) {
            return false;
        }
    }
}
