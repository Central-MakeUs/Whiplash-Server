package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;

@Getter
public enum AlarmFixture {

    ALARM_01(1L, "출근", LocalTime.of(7, 30), List.of(Weekday.MONDAY, Weekday.TUESDAY), SoundType.ONE, 37.5665, 126.9780, "서울특별시 중구 퇴계로 123", MemberFixture.MEMBER_1),
    ALARM_02(2L, "헬스장", LocalTime.of(8, 0), List.of(Weekday.TUESDAY, Weekday.THURSDAY), SoundType.TWO, 37.4980, 127.0276, "서울특별시 강남구 테헤란로 456", MemberFixture.MEMBER_2),
    ALARM_03(3L, "산책", LocalTime.of(6, 50), List.of(Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY), SoundType.FOUR, 37.5563, 126.9357, "서울특별시 마포구 월드컵북로 54", MemberFixture.MEMBER_3),
    ALARM_04(4L, "회의", LocalTime.of(9, 0), List.of(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY), SoundType.THREE, 37.5172, 126.9014, "서울특별시 영등포구 여의대로 128", MemberFixture.MEMBER_4),
    ALARM_05(5L, "출근", LocalTime.of(7, 10), List.of(Weekday.MONDAY, Weekday.FRIDAY), SoundType.ONE, 37.5796, 126.9770, "서울특별시 종로구 사직로 9", MemberFixture.MEMBER_5),
    ALARM_06(6L, "조깅", LocalTime.of(7, 45), List.of(Weekday.SATURDAY, Weekday.SUNDAY), SoundType.NONE, 37.5662, 126.9910, "서울특별시 중구 명동길 10", MemberFixture.MEMBER_6),
    ALARM_07(7L, "등교", LocalTime.of(6, 30), List.of(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY), SoundType.TWO, 37.6019, 127.0413, "서울특별시 성북구 정릉로 77", MemberFixture.MEMBER_7),
    ALARM_08(8L, "병원 예약", LocalTime.of(10, 0), List.of(Weekday.THURSDAY), SoundType.THREE, 37.5163, 127.1300, "서울특별시 송파구 올림픽로 300", MemberFixture.MEMBER_8),
    ALARM_09(9L, "업무 시작", LocalTime.of(8, 20), List.of(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY), SoundType.ONE, 37.5604, 126.9959, "서울특별시 중구 세종대로 110", MemberFixture.MEMBER_9),
    ALARM_10(10L, "출근", LocalTime.of(7, 5), List.of(Weekday.MONDAY, Weekday.FRIDAY), SoundType.FOUR, 37.4847, 126.9294, "서울특별시 관악구 남부순환로 1820", MemberFixture.MEMBER_10),

    ALARM_11(11L, "출근", LocalTime.of(6, 40), List.of(Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY), SoundType.ONE, 37.4968, 127.0137, "서울특별시 서초구 서초대로 396", MemberFixture.MEMBER_11),
    ALARM_12(12L, "출근", LocalTime.of(8, 15), List.of(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY), SoundType.TWO, 37.5124, 127.0588, "서울특별시 강남구 삼성로 212", MemberFixture.MEMBER_12),
    ALARM_13(13L, "출근", LocalTime.of(7, 0), List.of(Weekday.TUESDAY, Weekday.THURSDAY), SoundType.THREE, 37.5141, 126.9438, "서울특별시 동작구 상도로 369", MemberFixture.MEMBER_13),
    ALARM_14(14L, "약속", LocalTime.of(9, 20), List.of(Weekday.SATURDAY), SoundType.FOUR, 37.5341, 127.0054, "서울특별시 용산구 한강대로 23길", MemberFixture.MEMBER_14),
    ALARM_15(15L, "점심 약속", LocalTime.of(11, 0), List.of(Weekday.FRIDAY), SoundType.NONE, 37.5730, 126.9794, "서울특별시 종로구 종로1길 50", MemberFixture.MEMBER_15),
    ALARM_16(16L, "운동", LocalTime.of(6, 10), List.of(Weekday.MONDAY, Weekday.FRIDAY), SoundType.TWO, 37.5495, 126.9410, "서울특별시 마포구 양화로 45", MemberFixture.MEMBER_16),
    ALARM_17(17L, "병원", LocalTime.of(8, 50), List.of(Weekday.MONDAY), SoundType.THREE, 37.5303, 127.1239, "서울특별시 송파구 백제고분로 123", MemberFixture.MEMBER_17),
    ALARM_18(18L, "산책", LocalTime.of(5, 50), List.of(Weekday.SATURDAY, Weekday.SUNDAY), SoundType.ONE, 37.6452, 127.0121, "서울특별시 강북구 도봉로 220", MemberFixture.MEMBER_18),
    ALARM_19(19L, "출근", LocalTime.of(6, 25), List.of(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY), SoundType.FOUR, 37.5991, 126.9267, "서울특별시 서대문구 연세로 50", MemberFixture.MEMBER_19),
    ALARM_20(20L, "학교", LocalTime.of(7, 35), List.of(Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY), SoundType.TWO, 37.6086, 127.0293, "서울특별시 성북구 고려대로 45", MemberFixture.MEMBER_20);

    private final Long id;
    private final String alarmPurpose;
    private final LocalTime time;
    private final List<Weekday> repeatDays;
    private final SoundType soundType;
    private final double latitude;
    private final double longitude;
    private final String address;
    private final MemberFixture member;

    AlarmFixture(
        Long id,
        String alarmPurpose,
        LocalTime time,
        List<Weekday> repeatDays,
        SoundType soundType,
        double latitude,
        double longitude,
        String address,
        MemberFixture member
    ) {
        this.id = id;
        this.alarmPurpose = alarmPurpose;
        this.time = time;
        this.repeatDays = repeatDays;
        this.soundType = soundType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.member = member;
    }

    public AlarmEntity toMockEntity() {
        return AlarmEntity.builder()
            .id(id)
            .alarmPurpose(alarmPurpose)
            .time(time)
            .repeatDays(repeatDays)
            .soundType(soundType)
            .latitude(latitude)
            .longitude(longitude)
            .address(address)
            .member(member.toEntity())
            .build();
    }


    public AlarmEntity toEntity(MemberEntity member) {
        return AlarmEntity.builder()
            .alarmPurpose(alarmPurpose)
            .time(time)
            .repeatDays(repeatDays)
            .soundType(soundType)
            .latitude(latitude)
            .longitude(longitude)
            .address(address)
            .member(member)
            .build();
    }
}
