package akuma.whiplash.global.config.executor;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class LocationPreparationExecutorConfig {

    private static final int WORKER_COUNT = 16;
    private static final int QUEUE_CAPACITY = 16;

    @Bean("locationPreparationExecutor")
    public ThreadPoolTaskExecutor locationPreparationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(WORKER_COUNT);
        executor.setMaxPoolSize(WORKER_COUNT);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("location-preparation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
