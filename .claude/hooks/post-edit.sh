#!/bin/bash
# PostToolUse Hook — Java 파일 수정 시 semgrep 규칙 검사

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

# 파일 경로가 없으면 종료
if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# Java 파일이 아니면 패스
if ! echo "$FILE_PATH" | grep -qE '\.java$'; then
  exit 0
fi

# semgrep이 설치되어 있지 않으면 패스
if ! command -v semgrep &>/dev/null; then
  exit 0
fi

# .semgrep/semgrep.yml 이 없으면 패스
if [ ! -f ".semgrep/semgrep.yml" ]; then
  exit 0
fi

# semgrep 실행
RESULT=$(semgrep --config .semgrep/semgrep.yml "$FILE_PATH" --json 2>/dev/null)

if [ $? -ne 0 ] || [ -z "$RESULT" ]; then
  exit 0
fi

ERROR_COUNT=$(echo "$RESULT" | jq '[.results[] | select(.extra.severity == "ERROR")] | length' 2>/dev/null || echo "0")
WARN_COUNT=$(echo "$RESULT" | jq '[.results[] | select(.extra.severity == "WARNING")] | length' 2>/dev/null || echo "0")

if [ "$ERROR_COUNT" -gt 0 ] || [ "$WARN_COUNT" -gt 0 ]; then
  echo "" >&2
  echo "⚠️  semgrep 검사 결과 (${FILE_PATH})" >&2

  if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "  ❌ ERROR ${ERROR_COUNT}건:" >&2
    echo "$RESULT" | jq -r '[.results[] | select(.extra.severity == "ERROR")] | .[] | "    - [\(.check_id)] \(.extra.message) (line \(.start.line))"' 2>/dev/null >&2
  fi

  if [ "$WARN_COUNT" -gt 0 ]; then
    echo "  ⚠️  WARNING ${WARN_COUNT}건:" >&2
    echo "$RESULT" | jq -r '[.results[] | select(.extra.severity == "WARNING")] | .[] | "    - [\(.check_id)] \(.extra.message) (line \(.start.line))"' 2>/dev/null >&2
  fi
fi

# 경고만 출력, 작업은 차단하지 않음 (차단하려면 exit 2 사용)
exit 0
