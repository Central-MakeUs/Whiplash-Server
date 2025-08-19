package akuma.whiplash.global.config.sentry;

import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            if (event.getExceptions() != null && !event.getExceptions().isEmpty()) {
                var ex = event.getExceptions().get(0);
                if (ex.getType() != null && ex.getType().endsWith("ApplicationException")) {
                    // 전역 핸들러에서 미리 심어둔 태그
                    String code = event.getTag("error.code");
                    if (code != null) {
                        String originalMessage = ex.getValue(); // 기존 사람이 읽는 메시지
                        // 타이틀에 반영되는 'type'을 에러코드로 교체
                        ex.setType(code);
                        // 부제목(value)은 원래 메시지를 유지(또는 필요 시 축약)
                        ex.setValue(originalMessage);
                        // 참고용으로 원본도 extra에 보존
                        event.setExtra("original.message", originalMessage);
                    }
                }
            }
            return event;
        };
    }
}