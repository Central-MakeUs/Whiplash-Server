package akuma.whiplash.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushTargetDto;
import akuma.whiplash.infrastructure.redis.RedisService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("FcmService Unit Test")
@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;
    @Mock
    private RedisService redisService;

    @Nested
    @DisplayName("registerFcmToken - FCM 토큰 등록")
    class RegisterFcmTokenTest {

        @Test
        @DisplayName("성공: Redis에 토큰을 등록한다")
        void success() {
            // given
            Long memberId = 1L;
            String deviceId = "device1";
            String token = "token1";

            // when
            fcmService.registerFcmToken(memberId, deviceId, token);

            // then
            verify(redisService).upsertFcmToken(memberId, deviceId, token);
        }

        @Test
        @DisplayName("실패: Redis 오류 발생 시 예외를 전파한다")
        void fail_redisError() {
            // given
            Long memberId = 1L;
            String deviceId = "device2";
            String token = "token2";
            doThrow(new RuntimeException("redis error"))
                .when(redisService).upsertFcmToken(memberId, deviceId, token);

            // when & then
            assertThatThrownBy(() -> fcmService.registerFcmToken(memberId, deviceId, token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("redis error");
        }
    }

    @Test
    @DisplayName("성공: 사전 알림마다 notification과 문자열 data를 함께 전송한다")
    void success_sendsNotificationAndDataForEachOccurrence() throws Exception {
        // given
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse sendResponse = mock(SendResponse.class);
        givenBatchSuccess(batchResponse, sendResponse);
        ArgumentCaptor<MulticastMessage> messageCaptor = ArgumentCaptor.forClass(MulticastMessage.class);

        try (MockedStatic<FirebaseMessaging> firebaseMessagingStatic = Mockito.mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            org.mockito.BDDMockito.given(firebaseMessaging.sendEachForMulticast(messageCaptor.capture()))
                .willReturn(batchResponse);

            // when
            fcmService.sendBulkNotification(List.of(
                PushTargetDto.builder().token("token-a").memberId(1L).alarmId(10L).occurrenceId(100L).build(),
                PushTargetDto.builder().token("token-b").memberId(1L).alarmId(10L).occurrenceId(101L).build()
            ));

            // then
            verify(firebaseMessaging, times(2)).sendEachForMulticast(org.mockito.ArgumentMatchers.any());
            assertThat(messageCaptor.getAllValues())
                .allSatisfy(message -> {
                    assertThat(notificationOf(message)).isNotNull();
                    assertThat(dataOf(message)).isEqualTo(Map.of(
                        "type", "ALARM_PRE_NOTIFICATION",
                        "alarmId", "10",
                        "occurrenceId", dataOf(message).get("occurrenceId")
                    ));
                });
            assertThat(messageCaptor.getAllValues())
                .extracting(message -> dataOf(message).get("occurrenceId"))
                .containsExactlyInAnyOrder("100", "101");
        }
    }

    @Test
    @DisplayName("성공: 알람 울림 푸시에도 notification과 문자열 data를 함께 전송한다")
    void success_sendsNotificationAndDataForRingingAlarm() throws Exception {
        // given
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse sendResponse = mock(SendResponse.class);
        givenBatchSuccess(batchResponse, sendResponse);
        ArgumentCaptor<MulticastMessage> messageCaptor = ArgumentCaptor.forClass(MulticastMessage.class);

        try (MockedStatic<FirebaseMessaging> firebaseMessagingStatic = Mockito.mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            org.mockito.BDDMockito.given(firebaseMessaging.sendEachForMulticast(messageCaptor.capture()))
                .willReturn(batchResponse);

            // when
            fcmService.sendRingingNotifications(List.of(
                RingingPushTargetDto.builder()
                    .token("token-a").memberId(1L).alarmId(10L).occurrenceId(100L).build()
            ));

            // then
            assertThat(notificationOf(messageCaptor.getValue())).isNotNull();
            assertThat(dataOf(messageCaptor.getValue())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "ALARM_RINGING",
                "alarmId", "10",
                "occurrenceId", "100"
            ));
        }
    }

    private void givenBatchSuccess(BatchResponse batchResponse, SendResponse sendResponse) {
        org.mockito.BDDMockito.given(sendResponse.isSuccessful()).willReturn(true);
        org.mockito.BDDMockito.given(batchResponse.getResponses()).willReturn(List.of(sendResponse));
        org.mockito.BDDMockito.given(batchResponse.getSuccessCount()).willReturn(1);
        org.mockito.BDDMockito.given(batchResponse.getFailureCount()).willReturn(0);
    }

    private Object messageOf(MulticastMessage multicastMessage) {
        try {
            Method getMessageList = MulticastMessage.class.getDeclaredMethod("getMessageList");
            getMessageList.setAccessible(true);
            return ((List<?>) getMessageList.invoke(multicastMessage)).get(0);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("FCM 전송 메시지를 검사할 수 없습니다.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> dataOf(MulticastMessage multicastMessage) {
        return (Map<String, String>) fieldValue(messageOf(multicastMessage), "data");
    }

    private Object notificationOf(MulticastMessage multicastMessage) {
        return fieldValue(messageOf(multicastMessage), "notification");
    }

    private Object fieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("FCM 전송 메시지를 검사할 수 없습니다.", e);
        }
    }
}
