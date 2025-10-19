package akuma.whiplash.domains.auth.presentation.util.annotation;

import akuma.whiplash.domains.auth.presentation.util.validator.SocialTypeFormatValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SocialTypeFormatValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SocialTypeFormat {

    String message() default "유효하지 않은 소셜 로그인 유형입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] acceptedSocialTypes();
}