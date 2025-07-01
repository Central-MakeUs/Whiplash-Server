package akuma.whiplash.global.annotation.swagger;

import akuma.whiplash.global.response.code.CommonErrorCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomErrorCodes {

    CommonErrorCode[] commonErrorCodes() default {};
}