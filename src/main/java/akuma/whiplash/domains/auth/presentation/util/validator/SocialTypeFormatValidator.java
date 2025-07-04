package akuma.whiplash.domains.auth.presentation.util.validator;

import akuma.whiplash.domains.auth.presentation.util.annotation.SocialTypeFormat;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SocialTypeFormatValidator implements ConstraintValidator<SocialTypeFormat, String> {

    private Set<String> acceptedSocialTypes;

    @Override
    public void initialize(SocialTypeFormat constraintAnnotation) {
        String[] types = constraintAnnotation.acceptedSocialTypes();
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException("acceptedSocialTypes must not be null or empty");
        }

        acceptedSocialTypes = Arrays.stream(types)
            .map(String::toUpperCase) // 대소문자 구분 없이 검증하고 싶다면
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean isValid = value != null && acceptedSocialTypes.contains(value.toUpperCase());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            String allowedTypes = String.join(", ", acceptedSocialTypes);
            String errorMessage = String.format("지원하지 않는 소셜 타입입니다. (허용된 타입: %s)", allowedTypes);
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        return isValid;
    }
}
