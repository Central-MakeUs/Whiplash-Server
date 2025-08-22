package akuma.whiplash.global.log;

/**
 * 같은 스레드 체인(스케줄러 → 서비스 → 리포지토리 등)에서
 * 메서드 로깅을 잠시 비활성화하기 위한 ThreadLocal 플래그.
 */
public final class MethodLogSuppressor {
    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);

    private MethodLogSuppressor() {}

    public static void enable() { SUPPRESSED.set(true); }

    public static void disable() { SUPPRESSED.set(false); }

    public static boolean isSuppressed() { return SUPPRESSED.get(); }
}
