package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import lombok.Getter;

@Getter
public enum MemberFixture {

    MEMBER_1(1L, SocialType.GOOGLE, "001", "user001@example.com", "홍길동", Role.USER),
    MEMBER_2(2L, SocialType.KAKAO, "002", "user002@example.com", "김철수", Role.USER),
    MEMBER_3(3L, SocialType.APPLE, "003", "user003@example.com", "이영희", Role.USER),
    MEMBER_4(4L, SocialType.GOOGLE, "004", "user004@example.com", "박민수", Role.USER),
    MEMBER_5(5L, SocialType.KAKAO, "005", "user005@example.com", "최수진", Role.USER),
    MEMBER_6(6L, SocialType.APPLE, "006", "user006@example.com", "장동건", Role.USER),
    MEMBER_7(7L, SocialType.GOOGLE, "007", "user007@example.com", "고소영", Role.USER),
    MEMBER_8(8L, SocialType.KAKAO, "008", "user008@example.com", "배수지", Role.USER),
    MEMBER_9(9L, SocialType.APPLE, "009", "user009@example.com", "김연아", Role.USER),
    MEMBER_10(10L, SocialType.GOOGLE, "010", "user010@example.com", "윤아름", Role.USER),
    MEMBER_11(11L, SocialType.KAKAO, "011", "user011@example.com", "조정석", Role.USER),
    MEMBER_12(12L, SocialType.APPLE, "012", "user012@example.com", "유인나", Role.USER),
    MEMBER_13(13L, SocialType.GOOGLE, "013", "user013@example.com", "서강준", Role.USER),
    MEMBER_14(14L, SocialType.KAKAO, "014", "user014@example.com", "김태희", Role.USER),
    MEMBER_15(15L, SocialType.APPLE, "015", "user015@example.com", "한가인", Role.USER),
    MEMBER_16(16L, SocialType.GOOGLE, "016", "user016@example.com", "조보아", Role.USER),
    MEMBER_17(17L, SocialType.KAKAO, "017", "user017@example.com", "남궁민", Role.USER),
    MEMBER_18(18L, SocialType.APPLE, "018", "user018@example.com", "박보영", Role.USER),
    MEMBER_19(19L, SocialType.GOOGLE, "019", "user019@example.com", "김세정", Role.USER),
    MEMBER_20(20L, SocialType.KAKAO, "020", "user020@example.com", "서지혜", Role.USER);

    private final Long id;
    private final SocialType provider;
    private final String providerUserId;
    private final String email;
    private final String nickname;
    private final Role role;

    MemberFixture(Long id, SocialType provider, String providerUserId, String email, String nickname, Role role) {
        this.id = id;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public MemberEntity toMockEntity() {
        return MemberEntity.builder()
            .id(id)
            .provider(provider)
            .providerUserId(providerUserId)
            .email(email)
            .nickname(nickname)
            .role(role)
            .build();
    }

    public MemberEntity toEntity() {
        return MemberEntity.builder()
            .provider(provider)
            .providerUserId(providerUserId)
            .email(email)
            .nickname(nickname)
            .role(role)
            .build();
    }
}
