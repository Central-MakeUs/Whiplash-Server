package akuma.whiplash.global.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * prod 이외의 프로필에서는 아카이빙 X
 */
@Service
@Profile("!prod")
public class NoOpArchiveService implements ArchiveService {

    @Override
    public void archiveMemberWithRelations(Long memberId) {
        // 운영 환경 이외에서는 아카이빙 X
    }

    @Override
    public void archiveAlarmWithRelations(Long alarmId) {
        // 운영 환경 이외에서는 아카이빙 X
    }
}