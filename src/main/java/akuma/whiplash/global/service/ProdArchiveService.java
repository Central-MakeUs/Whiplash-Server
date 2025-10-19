package akuma.whiplash.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class ProdArchiveService implements ArchiveService {

    private final JdbcTemplate jdbcTemplate;

    // member -------------------------------------------------------------

    private static final String ARCHIVE_MEMBER_SQL = """
        INSERT INTO deleted_member (
            original_id, social_id, email, nickname, role,
            privacy_policy, push_notification_policy,
            privacy_agreed_at, push_agreed_at, created_at, updated_at
        )
        SELECT m.id, m.social_id, m.email, m.nickname, m.role,
               m.privacy_policy, m.push_notification_policy,
               m.privacy_agreed_at, m.push_agreed_at, m.created_at, m.updated_at
        FROM member m
        WHERE m.id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_member dm
                WHERE dm.original_id = m.id
          )
        """;

    // alarm --------------------------------------------------------------

    private static final String ARCHIVE_ALARM_BY_ID_SQL = """
        INSERT INTO deleted_alarm (
            original_id, member_id, alarm_purpose, time, repeat_days, sound_type,
            latitude, longitude, address, created_at, updated_at
        )
        SELECT a.id, a.member_id, a.alarm_purpose, a.time, a.repeat_days, a.sound_type,
               a.latitude, a.longitude, a.address, a.created_at, a.updated_at
        FROM alarm a
        WHERE a.id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm da
                WHERE da.original_id = a.id
          )
        """;

    private static final String ARCHIVE_ALARM_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm (
            original_id, member_id, alarm_purpose, time, repeat_days, sound_type,
            latitude, longitude, address, created_at, updated_at
        )
        SELECT a.id, a.member_id, a.alarm_purpose, a.time, a.repeat_days, a.sound_type,
               a.latitude, a.longitude, a.address, a.created_at, a.updated_at
        FROM alarm a
        WHERE a.member_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm da
                WHERE da.original_id = a.id
          )
        """;

    // alarm_occurrence ---------------------------------------------------

    private static final String ARCHIVE_ALARM_OCCURRENCE_BY_ALARM_SQL = """
        INSERT INTO deleted_alarm_occurrence (
            original_id, alarm_id, date, time, deactivate_type, deactivated_at,
            checkin_time, alarm_ringing, ringing_count, reminder_sent, created_at, updated_at
        )
        SELECT ao.id, ao.alarm_id, ao.date, ao.time, ao.deactivate_type, ao.deactivated_at,
               ao.checkin_time, ao.alarm_ringing, ao.ringing_count, ao.reminder_sent,
               ao.created_at, ao.updated_at
        FROM alarm_occurrence ao
        WHERE ao.alarm_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm_occurrence dao
                WHERE dao.original_id = ao.id
          )
        """;

    private static final String ARCHIVE_ALARM_OCCURRENCE_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm_occurrence (
            original_id, alarm_id, date, time, deactivate_type, deactivated_at,
            checkin_time, alarm_ringing, ringing_count, reminder_sent, created_at, updated_at
        )
        SELECT ao.id, ao.alarm_id, ao.date, ao.time, ao.deactivate_type, ao.deactivated_at,
               ao.checkin_time, ao.alarm_ringing, ao.ringing_count, ao.reminder_sent,
               ao.created_at, ao.updated_at
        FROM alarm_occurrence ao
        JOIN alarm a ON ao.alarm_id = a.id
        WHERE a.member_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm_occurrence dao
                WHERE dao.original_id = ao.id
          )
        """;

    // alarm_ringing_log --------------------------------------------------

    private static final String ARCHIVE_ALARM_RINGING_LOG_BY_ALARM_SQL = """
        INSERT INTO deleted_alarm_ringing_log (
            original_id, alarm_occurrence_id, ring_index, ringed_at, created_at, updated_at
        )
        SELECT arl.id, arl.alarm_occurrence_id, arl.ring_index, arl.ringed_at,
               arl.created_at, arl.updated_at
        FROM alarm_ringing_log arl
        JOIN alarm_occurrence ao ON arl.alarm_occurrence_id = ao.id
        WHERE ao.alarm_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm_ringing_log darl
                WHERE darl.original_id = arl.id
          )
        """;

    private static final String ARCHIVE_ALARM_RINGING_LOG_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm_ringing_log (
            original_id, alarm_occurrence_id, ring_index, ringed_at, created_at, updated_at
        )
        SELECT arl.id, arl.alarm_occurrence_id, arl.ring_index, arl.ringed_at,
               arl.created_at, arl.updated_at
        FROM alarm_ringing_log arl
        JOIN alarm_occurrence ao ON arl.alarm_occurrence_id = ao.id
        JOIN alarm a ON ao.alarm_id = a.id
        WHERE a.member_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm_ringing_log darl
                WHERE darl.original_id = arl.id
          )
        """;

    // alarm_off_log ------------------------------------------------------

    private static final String ARCHIVE_ALARM_OFF_LOG_BY_ALARM_SQL = """
        INSERT INTO deleted_alarm_off_log (
            original_id, alarm_id, member_id, created_at, updated_at
        )
        SELECT aol.id, aol.alarm_id, aol.member_id, aol.created_at, aol.updated_at
        FROM alarm_off_log aol
        WHERE aol.alarm_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm_off_log daol
                WHERE daol.original_id = aol.id
          )
        """;

    private static final String ARCHIVE_ALARM_OFF_LOG_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm_off_log (
            original_id, alarm_id, member_id, created_at, updated_at
        )
        SELECT aol.id, aol.alarm_id, aol.member_id, aol.created_at, aol.updated_at
        FROM alarm_off_log aol
        WHERE aol.member_id = ?
          AND NOT EXISTS (
                SELECT 1 FROM deleted_alarm_off_log daol
                WHERE daol.original_id = aol.id
          )
        """;

    // 트랜잭션 -----------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void archiveMemberWithRelations(Long memberId) {
        // 자식 → 부모 순으로 보관
        jdbcTemplate.update(ARCHIVE_ALARM_RINGING_LOG_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_ALARM_OCCURRENCE_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_ALARM_OFF_LOG_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_ALARM_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_MEMBER_SQL, memberId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void archiveAlarmWithRelations(Long alarmId) {
        jdbcTemplate.update(ARCHIVE_ALARM_RINGING_LOG_BY_ALARM_SQL, alarmId);
        jdbcTemplate.update(ARCHIVE_ALARM_OCCURRENCE_BY_ALARM_SQL, alarmId);
        jdbcTemplate.update(ARCHIVE_ALARM_OFF_LOG_BY_ALARM_SQL, alarmId);
        jdbcTemplate.update(ARCHIVE_ALARM_BY_ID_SQL, alarmId);
    }
}
