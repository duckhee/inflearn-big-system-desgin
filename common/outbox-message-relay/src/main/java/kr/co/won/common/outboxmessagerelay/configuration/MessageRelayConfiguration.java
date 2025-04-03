package kr.co.won.common.outboxmessagerelay.configuration;

import kr.co.won.common.outboxmessagerelay.MessageRelay;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync // 비동기 처리를 위한 annotation
@Configuration
@ComponentScan(basePackageClasses = {MessageRelay.class})
@EnableScheduling
public class MessageRelayConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    /**
     * KafkaTemplate 에 대해서 만들어주는 Bean 등록
     *
     * @return
     */
    @Bean
    public KafkaTemplate<String, String> messageRelayKafkaTemplate() {
        Map<String, Object> configurationProducerMap = new HashMap<>();
        configurationProducerMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configurationProducerMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configurationProducerMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configurationProducerMap.put(ProducerConfig.ACKS_CONFIG, "all"); // 확인하기 위한 응답을 모두 사용한다고 설정을 한다.

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configurationProducerMap));
    }

    /**
     * 비동기적으로 카프카 이벤트를 전달을 하기 위한 Thread Pool 및 Thread 설정
     *
     * @return
     */
    @Bean
    public Executor messageRelayPublishEventExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(20);
        taskExecutor.setMaxPoolSize(50);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("mr-pub-event-");
        return taskExecutor;
    }

    /**
     * 미전송된 이벤트에 대해서 처리를 하기 위한 SingleThreadExecutor
     *
     * @return
     */
    @Bean(name = "messageRelayPublishPendingEventExecutor")
    public Executor messageRelayPublishPendingEventExecutor() {
//         각 어플리케이션 서버마다 Shard가 조금씩 분활되어서 할당되어서 처리를 할 것이기 때문에 미전송이 된 이벤트 처리를 하나의 쓰레드로만 전송을 해준다.
        return Executors.newSingleThreadScheduledExecutor();
    }


}
