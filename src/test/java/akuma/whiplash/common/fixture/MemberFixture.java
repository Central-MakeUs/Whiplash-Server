package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.member.domain.contants.MemberStatus;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import lombok.Getter;

@Getter
public enum MemberFixture {

    MEMBER_1(1L, SocialType.GOOGLE, "001", "user001@example.com", "홍길동", Role.USER, MemberStatus.ACTIVE),
    MEMBER_2(2L, SocialType.KAKAO, "002", "user002@example.com", "김철수", Role.USER, MemberStatus.ACTIVE),
    MEMBER_3(3L, SocialType.APPLE, "003", "user003@example.com", "이영희", Role.USER, MemberStatus.ACTIVE),
    MEMBER_4(4L, SocialType.GOOGLE, "004", "user004@example.com", "박민수", Role.USER, MemberStatus.ACTIVE),
    MEMBER_5(5L, SocialType.KAKAO, "005", "user005@example.com", "최수진", Role.USER, MemberStatus.ACTIVE),
    MEMBER_6(6L, SocialType.APPLE, "006", "user006@example.com", "장동건", Role.USER, MemberStatus.ACTIVE),
    MEMBER_7(7L, SocialType.GOOGLE, "007", "user007@example.com", "고소영", Role.USER, MemberStatus.ACTIVE),
    MEMBER_8(8L, SocialType.KAKAO, "008", "user008@example.com", "배수지", Role.USER, MemberStatus.ACTIVE),
    MEMBER_9(9L, SocialType.APPLE, "009", "user009@example.com", "김연아", Role.USER, MemberStatus.ACTIVE),
    MEMBER_10(10L, SocialType.GOOGLE, "010", "user010@example.com", "윤아름", Role.USER, MemberStatus.ACTIVE),

    MEMBER_11(11L, SocialType.KAKAO, "011", "user011@example.com", "조정석", Role.USER, MemberStatus.ACTIVE),
    MEMBER_12(12L, SocialType.APPLE, "012", "user012@example.com", "유인나", Role.USER, MemberStatus.ACTIVE),
    MEMBER_13(13L, SocialType.GOOGLE, "013", "user013@example.com", "서강준", Role.USER, MemberStatus.ACTIVE),
    MEMBER_14(14L, SocialType.KAKAO, "014", "user014@example.com", "김태희", Role.USER, MemberStatus.ACTIVE),
    MEMBER_15(15L, SocialType.APPLE, "015", "user015@example.com", "한가인", Role.USER, MemberStatus.ACTIVE),
    MEMBER_16(16L, SocialType.GOOGLE, "016", "user016@example.com", "조보아", Role.USER, MemberStatus.ACTIVE),
    MEMBER_17(17L, SocialType.KAKAO, "017", "user017@example.com", "남궁민", Role.USER, MemberStatus.ACTIVE),
    MEMBER_18(18L, SocialType.APPLE, "018", "user018@example.com", "박보영", Role.USER, MemberStatus.ACTIVE),
    MEMBER_19(19L, SocialType.GOOGLE, "019", "user019@example.com", "김세정", Role.USER, MemberStatus.ACTIVE),
    MEMBER_20(20L, SocialType.KAKAO, "020", "user020@example.com", "서지혜", Role.USER, MemberStatus.ACTIVE);

    private final Long id;
    private final SocialType provider;
    private final String providerUserId;
    private final String email;
    private final String nickname;
    private final Role role;
    private final MemberStatus status;

    MemberFixture(
        Long id,
        SocialType provider,
        String providerUserId,
        String email,
        String nickname,
        Role role,
        MemberStatus status
    ) {
        this.id = id;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    public MemberEntity toMockEntity() {
        return MemberEntity.builder()
            .id(id)
            .provider(provider)
            .providerUserId(providerUserId)
            .email(email)
            .nickname(nickname)
            .role(role)
            .status(status)
            .build();
    }

    // 영속성 계층 테스트에서 사용하는 메서드, 실제 엔티티 세팅하므로 PK는 따로 세팅 X
    public MemberEntity toEntity() {
        return MemberEntity.builder()
            .provider(provider)
            .providerUserId(providerUserId)
            .email(email)
            .nickname(nickname)
            .role(role)
            .status(status)
            .build();
    }
}
