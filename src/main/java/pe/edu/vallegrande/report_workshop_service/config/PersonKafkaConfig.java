package pe.edu.vallegrande.report_workshop_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import pe.edu.vallegrande.report_workshop_service.model.event.PersonEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PersonKafkaConfig {

    @Value("${person-kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${person-kafka.username}")
    private String username;

    @Value("${person-kafka.password}")
    private String password;

    @Value("${person-kafka.group-id}")
    private String groupId;

    @Bean(name = "personKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PersonEvent> personKafkaListenerContainerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // NO INCLUIR ESTO AQUÍ si usas .addTrustedPackages() más abajo:
        // config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Configuración SASL_SSL para Confluent Cloud
        config.put("security.protocol", "SASL_SSL");
        config.put("sasl.mechanism", "PLAIN");
        config.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + username + "\" password=\"" + password + "\";");

        // Configura el deserializador directamente (sin confiar en propiedades duplicadas)
        JsonDeserializer<PersonEvent> deserializer = new JsonDeserializer<>(PersonEvent.class, false);
        deserializer.addTrustedPackages("pe.edu.vallegrande.report_workshop_service.model.event");

        DefaultKafkaConsumerFactory<String, PersonEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);

        ConcurrentKafkaListenerContainerFactory<String, PersonEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}
