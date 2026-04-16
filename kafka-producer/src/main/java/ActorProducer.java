import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Actor;
import util.AppConfig;

import java.util.Properties;

public class ActorProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ActorProducer.class);

    private static final String BROKER = AppConfig.get("kafka.broker",      "localhost:9092");
    private static final String TOPIC  = AppConfig.get("kafka.topic.actor", "ActorTopic");

    private final Producer<String, String> producer;
    private final Actor actor;

    public ActorProducer(Actor actor, Producer<String, String> producer) {
        this.actor    = actor;
        this.producer = producer;
    }

    public static Producer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      BROKER);
        props.put(ProducerConfig.CLIENT_ID_CONFIG,              "ActorProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG,                   "all");
        props.put(ProducerConfig.RETRIES_CONFIG,                3);
        return new KafkaProducer<>(props);
    }

    @Override
    public void run() {
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, actor.getId(), actor.jsonAsString());
        producer.send(record, (metadata, ex) -> {
            if (ex != null) {
                log.error("Failed to send actor {}: {}", actor.getId(), ex.getMessage());
            } else {
                log.info("Actor sent → topic={} partition={} offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}