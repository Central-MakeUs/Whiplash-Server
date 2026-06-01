import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * [시나리오 2] 무효 토큰(Invalid Token) 미제거 테스트
 * 
 * 목적: 
 * - 전송 실패하는 토큰이 Redis에서 삭제되지 않고 계속 재시도되는지 확인
 * - 실패 요청이 누적될 때 시스템 리소스 낭비 확인
 * 
 * 실행 흐름:
 * 1. setup(): 10명의 유저 생성
 * 2. invalid_token_load(): 각 유저가 "INVALID_"로 시작하는 토큰 등록 후 알람 울림
 */

export const options = {
  scenarios: {
    invalid_token_load: {
      executor: 'constant-vus',
      vus: 10,  // 10명의 유저가 지속적으로 실패 유발
      duration: '60s',
      exec: 'simulateInvalidTokens',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const users = [];
  for (let i = 1; i <= 10; i++) {
    const memberId = 3000 + i;
    const deviceId = `device-invalid-${memberId}`;
    
    const loginRes = http.post(`${BASE_URL}/api/dev/auth/login?memberId=${memberId}&deviceId=${deviceId}`);
    
    if (loginRes.status === 200) {
      users.push({
        memberId: memberId,
        accessToken: loginRes.json('result.accessToken'),
        deviceId: deviceId,
        // MockFcmService가 실패로 간주할 토큰 패턴
        fcmToken: `INVALID_TOKEN_${memberId}_${Date.now()}`
      });
    }
  }
  return { users };
}

let myAlarmId = null;

export function simulateInvalidTokens(data) {
  const user = data.users[(__VU - 1) % data.users.length];
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${user.accessToken}`
  };

  if (!myAlarmId) {
    // 1. 무효 토큰 등록
    http.post(`${BASE_URL}/api/auth/fcm-token`, JSON.stringify({ fcmToken: user.fcmToken }), { headers });

    // 2. 알람 생성
    const payload = JSON.stringify({
      address: "Invalid Loc",
      latitude: 37.5, longitude: 127.0,
      alarmPurpose: "Invalid Token Test",
      time: "12:00",
      repeatDays: ["MONDAY", "TUESDAY", "WEDNESDAY"],
      soundType: "KARINA_SCOLDING"
    });

    const createRes = http.post(`${BASE_URL}/api/alarms`, payload, { headers });
    if (createRes.status === 200) {
      myAlarmId = createRes.json('result.alarmId');
    }
  }

  // 3. 알람 울림 (스케줄러가 이 알람을 가져가서 INVALID 토큰으로 전송 시도 -> 실패 반복 예상)
  const ringRes = http.post(`${BASE_URL}/api/alarms/${myAlarmId}/ring`, {}, { headers });
  
  check(ringRes, { 'Ring Success': (r) => r.status === 200 });

  // 로그 확인을 위해 충분히 대기
  sleep(5);
}
