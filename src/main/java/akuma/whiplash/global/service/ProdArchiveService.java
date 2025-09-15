package akuma.whiplash.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class ProdArchiveService implements ArchiveService {

    private final JdbcTemplate jdbcTemplate;

    private static final String ARCHIVE_MEMBER_SQL = """
        INSERT INTO deleted_member (
            original_id, social_id, email, nickname, role,
            privacy_policy, push_notification_policy,
            privacy_agreed_at, push_agreed_at, created_at, updated_at
        )
        SELECT id, social_id, email, nickname, role,
               privacy_policy, push_notification_policy,
               privacy_agreed_at, push_agreed_at, created_at, updated_at
        FROM member
        WHERE id = ?
        """;

    private static final String ARCHIVE_ALARM_BY_ID_SQL = """
        INSERT INTO deleted_alarm (
            original_id, member_id, alarm_purpose, time, repeat_days, sound_type,
            latitude, longitude, address, created_at, updated_at
        )
        SELECT id, member_id, alarm_purpose, time, repeat_days, sound_type,
               latitude, longitude, address, created_at, updated_at
        FROM alarm
        WHERE id = ?
        """;

    private static final String ARCHIVE_ALARM_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm (
            original_id, member_id, alarm_purpose, time, repeat_days, sound_type,
            latitude, longitude, address, created_at, updated_at
        )
        SELECT id, member_id, alarm_purpose, time, repeat_days, sound_type,
               latitude, longitude, address, created_at, updated_at
        FROM alarm
        WHERE member_id = ?
        """;

    private static final String ARCHIVE_ALARM_OCCURRENCE_BY_ALARM_SQL = """
        INSERT INTO deleted_alarm_occurrence (
            original_id, alarm_id, date, time, deactivate_type, deactivated_at,
            checkin_time, alarm_ringing, ringing_count, reminder_sent, created_at, updated_at
        )
        SELECT id, alarm_id, date, time, deactivate_type, deactivated_at,
               checkin_time, alarm_ringing, ringing_count, reminder_sent, created_at, updated_at
        FROM alarm_occurrence
        WHERE alarm_id = ?
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
        """;

    private static final String ARCHIVE_ALARM_RINGING_LOG_BY_ALARM_SQL = """
        INSERT INTO deleted_alarm_ringing_log (
            original_id, alarm_occurrence_id, ring_index, ringed_at, created_at, updated_at
        )
        SELECT id, alarm_occurrence_id, ring_index, ringed_at, created_at, updated_at
        FROM alarm_ringing_log
        WHERE alarm_occurrence_id IN (
            SELECT id FROM alarm_occurrence WHERE alarm_id = ?
        )
        """;

    private static final String ARCHIVE_ALARM_RINGING_LOG_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm_ringing_log (
            original_id, alarm_occurrence_id, ring_index, ringed_at, created_at, updated_at
        )
        SELECT arl.id, arl.alarm_occurrence_id, arl.ring_index, arl.ringed_at,
               arl.created_at, arl.updated_at
        FROM alarm_ringing_log arl
        WHERE arl.alarm_occurrence_id IN (
            SELECT ao.id FROM alarm_occurrence ao
            JOIN alarm a ON ao.alarm_id = a.id
            WHERE a.member_id = ?
        )
        """;

    private static final String ARCHIVE_ALARM_OFF_LOG_BY_ALARM_SQL = """
        INSERT INTO deleted_alarm_off_log (
            original_id, alarm_id, member_id, created_at, updated_at
        )
        SELECT id, alarm_id, member_id, created_at, updated_at
        FROM alarm_off_log
        WHERE alarm_id = ?
        """;

    private static final String ARCHIVE_ALARM_OFF_LOG_BY_MEMBER_SQL = """
        INSERT INTO deleted_alarm_off_log (
            original_id, alarm_id, member_id, created_at, updated_at
        )
        SELECT id, alarm_id, member_id, created_at, updated_at
        FROM alarm_off_log
        WHERE member_id = ?
        """;

    @Override
    public void archiveMemberWithRelations(Long memberId) {
        jdbcTemplate.update(ARCHIVE_ALARM_RINGING_LOG_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_ALARM_OCCURRENCE_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_ALARM_OFF_LOG_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_ALARM_BY_MEMBER_SQL, memberId);
        jdbcTemplate.update(ARCHIVE_MEMBER_SQL, memberId);
    }

    @Override
    public void archiveAlarmWithRelations(Long alarmId) {
        jdbcTemplate.update(ARCHIVE_ALARM_RINGING_LOG_BY_ALARM_SQL, alarmId);
        jdbcTemplate.update(ARCHIVE_ALARM_OCCURRENCE_BY_ALARM_SQL, alarmId);
        jdbcTemplate.update(ARCHIVE_ALARM_OFF_LOG_BY_ALARM_SQL, alarmId);
        jdbcTemplate.update(ARCHIVE_ALARM_BY_ID_SQL, alarmId);
    }
}
