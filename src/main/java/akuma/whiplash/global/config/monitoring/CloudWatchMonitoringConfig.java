package akuma.whiplash.global.config.monitoring;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.time.Duration;
import java.util.Properties;

@Configuration
public class CloudWatchMonitoringConfig {

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(DefaultCredentialsProvider.create()) 
            .build();
    }

    @Bean
    public CloudWatchConfig cloudWatchConfig() {
        Properties props = new Properties();
        props.put("cloudwatch.namespace", "Nuntteo/Dev");
        props.put("cloudwatch.step", Duration.ofMinutes(5).toString()); // 5분 간격으로 cloud watch에 데이터 전송
       
        return new CloudWatchConfig() {
            @Override public String get(String k) { return props.getProperty(k); }
        };
    }

    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(
            CloudWatchConfig config, Clock clock, CloudWatchAsyncClient client) {
        return new CloudWatchMeterRegistry(config, clock, client);
    }
}
