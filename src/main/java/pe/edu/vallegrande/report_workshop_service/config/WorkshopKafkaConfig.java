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
public class WorkshopKafkaConfig {

    // Variables externas desde application.yml o entorno
    @Value("${workshop-kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${workshop-kafka.username}")
    private String username;

    @Value("${workshop-kafka.password}")
    private String password;

    @Value("${workshop-kafka.group-id}")
    private String groupId;

    // Bean que define el contenedor de listeners para Kafka (talleres)
    @Bean(name = "workshopKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> workshopKafkaListenerContainerFactory() {
        return buildFactory();
    }

    // Crea la configuración de consumidor Kafka para Confluent
    private ConcurrentKafkaListenerContainerFactory<String, String> buildFactory() {
        Map<String, Object> config = new HashMap<>();

        // Configuración básica del consumidor
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Seguridad para conexión con Confluent Cloud
        config.put("security.protocol", "SASL_SSL");
        config.put("sasl.mechanism", "PLAIN");
        config.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + username + "\" password=\"" + password + "\";");

        // Crea y devuelve el contenedor Kafka con la configuración anterior
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(config);
        ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        listenerFactory.setConsumerFactory(consumerFactory);

        return listenerFactory;
    }
}
