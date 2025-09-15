package akuma.whiplash.global.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * prod 이외의 프로필에서는 아카이ㅡ빙 X
 */
@Service
@Profile("!prod")
public class NoOpArchiveService implements ArchiveService {

    @Override
    public void archiveMemberWithRelations(Long memberId) {
        // no-op for non-prod profiles
    }

    @Override
    public void archiveAlarmWithRelations(Long alarmId) {
        // no-op for non-prod profiles
    }
}