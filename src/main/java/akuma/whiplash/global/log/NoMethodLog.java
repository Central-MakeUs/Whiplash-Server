package akuma.whiplash.global.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 붙은 메서드/클래스 및 그 호출 체인 동안 MethodLoggingAspect 로깅을 억제합니다.
 * - @NoMethodLog on TYPE  : 클래스 전체 억제
 * - @NoMethodLog on METHOD: 해당 메서드 억제
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NoMethodLog {}
