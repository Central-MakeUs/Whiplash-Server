package akuma.whiplash.domains.member.application.utils;

import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class NicknameGenerator {

    private static final String[] ADJECTIVES = {
        "행복한", "용감한", "똑똑한", "귀여운", "멋진", "배고픈", "느긋한", "씩씩한"
    };

    private static final String[] NOUNS = {
        "고양이", "강아지", "사자", "토끼", "여우", "판다", "너구리", "돌고래"
    };

    public String generate() {
        String adjective = ADJECTIVES[new Random().nextInt(ADJECTIVES.length)];
        String noun = NOUNS[new Random().nextInt(NOUNS.length)];
        int number = 100 + new Random().nextInt(900); // 100~999
        return adjective + noun + number;
    }
}