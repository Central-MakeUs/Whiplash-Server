-- Legacy soft-delete/archive tables are no longer used.
-- Member withdrawal and alarm deletion now remove related data directly.

DROP TABLE IF EXISTS deleted_alarm_off_log;
DROP TABLE IF EXISTS deleted_alarm_ringing_log;
DROP TABLE IF EXISTS deleted_alarm_occurrence;
DROP TABLE IF EXISTS deleted_alarm;
DROP TABLE IF EXISTS deleted_member;
