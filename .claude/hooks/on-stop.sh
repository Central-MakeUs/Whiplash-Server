#!/bin/bash
# Stop Hook — Claude 작업 완료 시 알림

# macOS 알림
if command -v osascript &>/dev/null; then
  osascript -e 'display notification "Claude 작업이 완료됐습니다." with title "Whiplash 🔔"' 2>/dev/null
fi

# Linux (notify-send)
if command -v notify-send &>/dev/null; then
  notify-send "Whiplash 🔔" "Claude 작업이 완료됐습니다." 2>/dev/null
fi

exit 0
