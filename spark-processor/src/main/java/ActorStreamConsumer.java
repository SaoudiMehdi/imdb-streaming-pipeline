import cassandra.CassandraConnector;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import config.AppConfig;
import kafka.serializer.StringDecoder;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ActorStreamConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ActorStreamConsumer.class);

    private static final String TOPIC          = AppConfig.get("kafka.topic.actor",       "ActorTopic");
    private static final String BROKER         = AppConfig.get("kafka.broker",             "localhost:9092");
    private static final String CASSANDRA_HOST = AppConfig.get("cassandra.host",           "localhost");
    private static final int    CASSANDRA_PORT = AppConfig.getInt("cassandra.port",        9042);
    private static final long   BATCH_DURATION = AppConfig.getInt("spark.batch.duration.ms", 20000);
    private static final String CHECKPOINT_DIR = AppConfig.get("spark.checkpoint.dir",    "/tmp/spark-checkpoint-actors");

    @Override
    public void run() {
        log.info("ActorStreamConsumer starting...");
        SparkConf conf = new SparkConf().setAppName("imdb-actor-consumer").setMaster("local[*]");
        JavaStreamingContext ssc = new JavaStreamingContext(conf, new Duration(BATCH_DURATION));
        ssc.checkpoint(CHECKPOINT_DIR);

        Map<String, String> kafkaParams = new HashMap<>();
        kafkaParams.put("metadata.broker.list", BROKER);

        CassandraConnector connector = new CassandraConnector();
        connector.connectdb(CASSANDRA_HOST, CASSANDRA_PORT);

        final String insertQuery =
            "INSERT INTO imdb_keyspace2.actors (idActor, name, birthDate, birthPlace, gender) VALUES (?,?,?,?,?)";
        PreparedStatement ps = connector.getSession().prepare(insertQuery);

        JavaPairInputDStream<String, String> stream = KafkaUtils.createDirectStream(
                ssc, String.class, String.class,
                StringDecoder.class, StringDecoder.class,
                kafkaParams, Collections.singleton(TOPIC));

        stream.foreachRDD(rdd -> {
            long count = rdd.count();
            if (count == 0) return;
            log.info("Actor batch: {} records across {} partitions", count, rdd.partitions().size());

            rdd.collect().forEach(record -> {
                try {
                    JSONObject json = new JSONObject(record._2);
                    String id         = json.getString("id");
                    String name       = json.getString("name");
                    String gender     = json.getString("gender");
                    String birthDate  = json.getString("birthDate");
                    String birthPlace = json.getString("birthPlace");

                    BoundStatement bs = ps.bind(id, name, birthDate, birthPlace, gender);
                    connector.getSession().execute(bs);
                } catch (Exception e) {
                    log.error("Failed to process actor record: {}", e.getMessage());
                }
            });
        });

        ssc.start();
        try {
            ssc.awaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            connector.close();
        }
    }

    public static void main(String[] args) {
        new ActorStreamConsumer().run();
    }
}