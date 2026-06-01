import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * [시나리오 1] 스레드 독점(Starvation) 테스트
 * 
 * 목적: 
 * - 대량의 동시 '알람 울림' 요청 발생 시 스케줄러가 10초 주기를 지키는지 확인
 * - FCM 전송 지연이 발생할 때(별도 주입 필요) 스레드 풀이 고갈되는 현상 관측
 * 
 * 실행 흐름:
 * 1. setup(): 50명의 유저 로그인 및 토큰 확보
 * 2. create_and_ring(): 각 VU가 할당된 유저로 알람을 생성하고 지속적으로 '울림' 상태 유지
 */

export const options = {
  scenarios: {
    // 1분 동안 30명의 동시 사용자가 알람을 울림 상태로 유지
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 30 }, // 10초 동안 30명까지 증가
        { duration: '50s', target: 30 }, // 50초 유지
        { duration: '10s', target: 0 },  // 종료
      ],
      gracefulRampDown: '5s',
      exec: 'stressRinging',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'], // API 응답은 2초 이내여야 함
    http_req_failed: ['rate<0.05'],    // 에러율 5% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 유틸: 랜덤 문자열
function randomString(length) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let res = '';
  for (let i = 0; i < length; i++) res += chars.charAt(Math.floor(Math.random() * chars.length));
  return res;
}

// 1. Setup: 50명의 유저 토큰 미리 확보
export function setup() {
  const users = [];
  // FCM 지연 시간 초기화 (0ms)
  http.post(`${BASE_URL}/api/dev/auth/fcm-delay?delay=0`);

  for (let i = 1; i <= 50; i++) {
    const memberId = 2000 + i;
    const deviceId = `device-starve-${memberId}`;
    
    const loginRes = http.post(`${BASE_URL}/api/dev/auth/login?memberId=${memberId}&deviceId=${deviceId}`);
    
    if (loginRes.status === 200) {
      const body = loginRes.json();
      users.push({
        memberId: memberId,
        accessToken: body.result.accessToken,
        deviceId: deviceId,
        fcmToken: `token-starve-${memberId}`
      });
    }
  }
  console.log(`[Setup] ${users.length} users prepared.`);
  return { users };
}

// VU별 상태 저장 (알람 ID 캐싱)
let myAlarmId = null;

// 2. Main Logic
export function stressRinging(data) {
  // 현재 VU에 매핑되는 유저 선택
  const userIndex = (__VU - 1) % data.users.length;
  const user = data.users[userIndex];
  
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${user.accessToken}`
  };

  // 2-1. 알람이 없으면 생성 (최초 1회)
  if (!myAlarmId) {
    // FCM 토큰 등록
    http.post(`${BASE_URL}/api/auth/fcm-token`, JSON.stringify({ fcmToken: user.fcmToken }), { headers });

    const payload = JSON.stringify({
      address: "Starvation Test Loc",
      latitude: 37.5, longitude: 127.0,
      alarmPurpose: `Starve Test ${randomString(5)}`,
      time: "12:00",
      repeatDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
      soundType: "KARINA_SCOLDING"
    });

    const createRes = http.post(`${BASE_URL}/api/alarms`, payload, { headers });
    
    if (createRes.status === 200) {
      myAlarmId = createRes.json('result.alarmId');
    } else {
      console.error(`User ${user.memberId} failed to create alarm`);
      sleep(1);
      return;
    }
  }

  // 2-2. 알람 울림 API 호출 (이미 울리고 있어도 호출하여 부하 유지)
  const ringRes = http.post(`${BASE_URL}/api/alarms/${myAlarmId}/ring`, {}, { headers });
  
  check(ringRes, {
    'Ring API 200': (r) => r.status === 200,
  });

  // 1초 대기 (스케줄러는 10초마다 돌지만, 유저는 계속 상태를 확인/갱신한다고 가정)
  sleep(1);
}
