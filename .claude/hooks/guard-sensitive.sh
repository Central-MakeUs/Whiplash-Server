#!/bin/bash
# PreToolUse Hook — 운영 환경 민감 파일 수정 차단

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

# 파일 경로가 없으면 통과
if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# 보호 대상 패턴 목록
PROTECTED_PATTERNS=(
  "application-prod"
  "env\.properties$"
  "whiplash-firebase-key\.json$"
  "deploy\.sh$"
  "Jenkinsfile$"
)

for PATTERN in "${PROTECTED_PATTERNS[@]}"; do
  if echo "$FILE_PATH" | grep -qE "$PATTERN"; then
    echo "🚫 보호된 파일입니다: $FILE_PATH" >&2
    echo "" >&2
    echo "운영 환경 설정 파일은 Claude Code를 통해 직접 수정할 수 없습니다." >&2
    echo "필요한 경우 직접 에디터에서 수정해 주세요." >&2
    exit 2
  fi
done

exit 0
