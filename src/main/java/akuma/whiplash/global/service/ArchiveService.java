package akuma.whiplash.global.service;

/**
 * 테이블 데이터 삭제 전 'deleted_테이블이름' 테이블로 삭제 데이터 이관
 */
public interface ArchiveService {

    void archiveMemberWithRelations(Long memberId);

    void archiveAlarmWithRelations(Long alarmId);
}
