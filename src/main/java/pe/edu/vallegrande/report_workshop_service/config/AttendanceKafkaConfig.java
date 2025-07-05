package pe.edu.vallegrande.report_workshop_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AttendanceKafkaConfig {

    @Value("${attendance-kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${attendance-kafka.username}")
    private String username;

    @Value("${attendance-kafka.password}")
    private String password;

    @Value("${attendance-kafka.group-id}")
    private String groupId;

    @Bean(name = "attendanceKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> attendanceKafkaListenerContainerFactory() {
        return buildFactory();
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> buildFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Configuración para Confluent Cloud con seguridad SASL_SSL
        config.put("security.protocol", "SASL_SSL");
        config.put("sasl.mechanism", "PLAIN");
        config.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + username + "\" password=\"" + password + "\";");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(config);
        ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        listenerFactory.setConsumerFactory(consumerFactory);

        return listenerFactory;
    }
}
