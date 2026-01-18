import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  iterations: 1, // Run once to populate data
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const userCount = 30; // Number of users to simulate

  for (let i = 1; i <= userCount; i++) {
    const memberId = 1000 + i;
    const deviceId = `device-${memberId}`;
    const fcmToken = `token-fake-${memberId}`; // Fake token to trigger errors (if needed)

    // 1. Dev Login
    let loginRes = http.post(`${BASE_URL}/api/dev/auth/login?memberId=${memberId}&deviceId=${deviceId}`);
    check(loginRes, { 'login status is 200': (r) => r.status === 200 });
    
    if (loginRes.status !== 200) {
        console.error(`Login failed for ${memberId}`);
        continue;
    }

    const accessToken = loginRes.json('result.accessToken');
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
    };

    // 2. Register FCM Token
    let fcmRes = http.post(`${BASE_URL}/api/auth/fcm-token`, JSON.stringify({
        fcmToken: fcmToken
    }), { headers: headers });
    check(fcmRes, { 'fcm register status is 200': (r) => r.status === 200 });

    // 3. Create Alarm
    const alarmPayload = {
        address: "Seoul Station",
        latitude: 37.5547,
        longitude: 126.9707,
        alarmPurpose: "Wake Up Call",
        time: "08:00",
        repeatDays: ["월", "화", "수", "목", "금"],
        soundType: "Basic"
    };

    let createRes = http.post(`${BASE_URL}/api/alarms`, JSON.stringify(alarmPayload), { headers: headers });
    check(createRes, { 'create alarm status is 200': (r) => r.status === 200 });

    if (createRes.status === 200) {
        const alarmId = createRes.json('result.alarmId');
        
        // 4. Ring Alarm
        let ringRes = http.post(`${BASE_URL}/api/alarms/${alarmId}/ring`, {}, { headers: headers });
        check(ringRes, { 'ring alarm status is 200': (r) => r.status === 200 });
        console.log(`User ${memberId}: Alarm ${alarmId} is now ringing.`);
    }

    sleep(0.1); // slight throttle
  }
}
