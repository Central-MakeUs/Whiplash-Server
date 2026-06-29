package akuma.whiplash.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LogUtils Unit Test")
class LogUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("maskSensitiveJson - JSON 민감 정보 마스킹")
    class MaskSensitiveJsonTest {

        @Test
        @DisplayName("성공: 좌표와 결제 식별자 및 토큰을 마스킹한다")
        void success() {
            // given
            String json = """
                {
                  "latitude": 37.4847,
                  "longitude": 126.9294,
                  "paymentId": "payment-success-001",
                  "adProofToken": "ad-proof-token",
                  "fcmToken": "fcm-token",
                  "nested": {
                    "deviceId": "device-uuid"
                  }
                }
                """;

            // when
            String result = LogUtils.maskSensitiveJson(objectMapper, json);

            // then
            assertThat(result).contains("\"latitude\":\"****\"");
            assertThat(result).contains("\"longitude\":\"****\"");
            assertThat(result).contains("\"paymentId\":\"****\"");
            assertThat(result).contains("\"adProofToken\":\"****\"");
            assertThat(result).contains("\"fcmToken\":\"****\"");
            assertThat(result).contains("\"deviceId\":\"****\"");
            assertThat(result).doesNotContain("37.4847");
            assertThat(result).doesNotContain("payment-success-001");
        }
    }

    @Nested
    @DisplayName("maskSensitiveQuery - query string 민감 정보 마스킹")
    class MaskSensitiveQueryTest {

        @Test
        @DisplayName("성공: query string의 좌표와 결제 식별자를 마스킹한다")
        void success() {
            // given
            String query = "latitude=37.4847&longitude=126.9294&paymentId=payment-success-001&query=cafe";

            // when
            String result = LogUtils.maskSensitiveQuery(query);

            // then
            assertThat(result).isEqualTo("latitude=****&longitude=****&paymentId=****&query=cafe");
        }
    }
}
