package akuma.whiplash.global.validation.annotation;

import akuma.whiplash.global.validation.validator.IanaTimeZoneFormatValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = IanaTimeZoneFormatValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IanaTimeZoneFormat {

    String message() default "유효하지 않은 IANA time zone입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
