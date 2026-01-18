import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * [시나리오 3] FCM 지연 동적 주입 (Dynamic Delay Injection)
 * 
 * 목적: 
 * - 시스템 운영 중 외부 API(FCM) 응답 속도가 변할 때 스케줄러 안정성 테스트
 * 
 * 구조:
 * - background_traffic: 20명의 유저가 평소처럼 알람을 울림 (부하 생성)
 * - delay_controller: 1명의 관리자 VU가 주기적으로 서버의 지연 설정을 변경 (0ms -> 200ms -> 1500ms -> 0ms)
 */

export const options = {
  scenarios: {
    background_traffic: {
      executor: 'constant-vus',
      vus: 20,
      duration: '90s',
      exec: 'trafficGenerator',
    },
    delay_controller: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '90s',
      exec: 'delayInjector',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const users = [];
  // 초기화
  http.post(`${BASE_URL}/api/dev/auth/fcm-delay?delay=0`);

  for (let i = 1; i <= 20; i++) {
    const memberId = 4000 + i;
    const res = http.post(`${BASE_URL}/api/dev/auth/login?memberId=${memberId}&deviceId=dev-${memberId}`);
    if (res.status === 200) {
      users.push({
        memberId,
        accessToken: res.json('result.accessToken'),
        fcmToken: `token-delay-${memberId}`
      });
    }
  }
  return { users };
}

let myAlarmId = null;

// [역할 1] 배경 트래픽 생성 (일반 유저)
export function trafficGenerator(data) {
  const user = data.users[(__VU - 1) % data.users.length];
  const headers = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${user.accessToken}` };

  if (!myAlarmId) {
    http.post(`${BASE_URL}/api/auth/fcm-token`, JSON.stringify({ fcmToken: user.fcmToken }), { headers });
    
    const payload = JSON.stringify({
      address: "Delay Test",
      latitude: 37.5, longitude: 127.0,
      alarmPurpose: "Delay Test",
      time: "12:00",
      repeatDays: ["월", "화", "수"],
      soundType: "Basic"
    });
    
    const res = http.post(`${BASE_URL}/api/alarms`, payload, { headers });
    if (res.status === 200) myAlarmId = res.json('result.alarmId');
  }

  if (myAlarmId) {
    http.post(`${BASE_URL}/api/alarms/${myAlarmId}/ring`, {}, { headers });
  }
  sleep(2);
}

// [역할 2] 지연 시간 조절 (관리자)
export function delayInjector() {
  console.log(">>> [Phase 1] Normal (0ms) - 20s");
  http.post(`${BASE_URL}/api/dev/auth/fcm-delay?delay=0`);
  sleep(20);

  console.log(">>> [Phase 2] Small Delay (200ms) - 20s");
  // 20 users * 200ms = 4s processing (Safe within 10s interval)
  http.post(`${BASE_URL}/api/dev/auth/fcm-delay?delay=200`);
  sleep(20);

  console.log(">>> [Phase 3] Critical Delay (800ms) - 30s");
  // 20 users * 800ms = 16s processing (> 10s interval -> Starvation expected)
  http.post(`${BASE_URL}/api/dev/auth/fcm-delay?delay=800`);
  sleep(30);

  console.log(">>> [Phase 4] Recovery (0ms)");
  http.post(`${BASE_URL}/api/dev/auth/fcm-delay?delay=0`);
  sleep(10);
}