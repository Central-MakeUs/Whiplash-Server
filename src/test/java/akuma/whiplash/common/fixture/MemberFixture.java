package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public enum MemberFixture {

    MEMBER_1(1L, "GOOGLE_001", "user001@example.com", "홍길동", Role.USER, true, true, ago(10), ago(10)),
    MEMBER_2(2L, "KAKAO_002", "user002@example.com", "김철수", Role.USER, true, true, ago(9), ago(9)),
    MEMBER_3(3L, "APPLE_003", "user003@example.com", "이영희", Role.USER, true, true, ago(8), ago(8)),
    MEMBER_4(4L, "GOOGLE_004", "user004@example.com", "박민수", Role.USER, true, true, ago(7), ago(7)),
    MEMBER_5(5L, "KAKAO_005", "user005@example.com", "최수진", Role.USER, true, true, ago(6), ago(6)),
    MEMBER_6(6L, "APPLE_006", "user006@example.com", "장동건", Role.USER, true, true, ago(6), ago(6)),
    MEMBER_7(7L, "GOOGLE_007", "user007@example.com", "고소영", Role.USER, true, true, ago(5), ago(5)),
    MEMBER_8(8L, "KAKAO_008", "user008@example.com", "배수지", Role.USER, true, true, ago(5), ago(5)),
    MEMBER_9(9L, "APPLE_009", "user009@example.com", "김연아", Role.USER, true, true, ago(4), ago(4)),
    MEMBER_10(10L, "GOOGLE_010", "user010@example.com", "윤아름", Role.USER, true, true, ago(4), ago(4)),

    MEMBER_11(11L, "KAKAO_011", "user011@example.com", "조정석", Role.USER, true, true, ago(3), ago(3)),
    MEMBER_12(12L, "APPLE_012", "user012@example.com", "유인나", Role.USER, true, true, ago(3), ago(3)),
    MEMBER_13(13L, "GOOGLE_013", "user013@example.com", "서강준", Role.USER, true, true, ago(2), ago(2)),
    MEMBER_14(14L, "KAKAO_014", "user014@example.com", "김태희", Role.USER, true, true, ago(2), ago(2)),
    MEMBER_15(15L, "APPLE_015", "user015@example.com", "한가인", Role.USER, true, true, ago(2), ago(2)),
    MEMBER_16(16L, "GOOGLE_016", "user016@example.com", "조보아", Role.USER, true, true, ago(1), ago(1)),
    MEMBER_17(17L, "KAKAO_017", "user017@example.com", "남궁민", Role.USER, true, true, ago(1), ago(1)),
    MEMBER_18(18L, "APPLE_018", "user018@example.com", "박보영", Role.USER, true, true, ago(1), ago(1)),
    MEMBER_19(19L, "GOOGLE_019", "user019@example.com", "김세정", Role.USER, true, true, ago(0), ago(0)),
    MEMBER_20(20L, "KAKAO_020", "user020@example.com", "서지혜", Role.USER, true, true, ago(0), ago(0));

    private final Long id;
    private final String socialId;
    private final String email;
    private final String nickname;
    private final Role role;
    private final boolean privacyPolicy;
    private final boolean pushNotificationPolicy;
    private final LocalDateTime privacyAgreedAt;
    private final LocalDateTime pushAgreedAt;

    MemberFixture(
        Long id,
        String socialId,
        String email,
        String nickname,
        Role role,
        boolean privacyPolicy,
        boolean pushNotificationPolicy,
        LocalDateTime privacyAgreedAt,
        LocalDateTime pushAgreedAt
    ) {
        this.id = id;
        this.socialId = socialId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.privacyPolicy = privacyPolicy;
        this.pushNotificationPolicy = pushNotificationPolicy;
        this.privacyAgreedAt = privacyAgreedAt;
        this.pushAgreedAt = pushAgreedAt;
    }

    public MemberEntity toMockEntity() {
        return MemberEntity.builder()
            .id(id)
            .socialId(socialId)
            .email(email)
            .nickname(nickname)
            .role(role)
            .privacyPolicy(privacyPolicy)
            .pushNotificationPolicy(pushNotificationPolicy)
            .privacyAgreedAt(privacyAgreedAt)
            .pushAgreedAt(pushAgreedAt)
            .build();
    }

    // 영속성 계층 테스트에서 사용하는 메서드, 실제 엔티티 세팅하므로 PK는 따로 세팅 X
    public MemberEntity toEntity() {
        return MemberEntity.builder()
            .socialId(socialId)
            .email(email)
            .nickname(nickname)
            .role(role)
            .privacyPolicy(privacyPolicy)
            .pushNotificationPolicy(pushNotificationPolicy)
            .privacyAgreedAt(privacyAgreedAt)
            .pushAgreedAt(pushAgreedAt)
            .build();
    }

    private static LocalDateTime ago(int daysAgo) {
        return LocalDateTime.now().minusDays(daysAgo);
    }
}
