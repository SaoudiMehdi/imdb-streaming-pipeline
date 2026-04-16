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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NewsStreamConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(NewsStreamConsumer.class);

    private static final String TOPIC            = AppConfig.get("kafka.topic.news",      "NewsTopic");
    private static final String BROKER           = AppConfig.get("kafka.broker",           "localhost:9092");
    private static final String CASSANDRA_HOST   = AppConfig.get("cassandra.host",         "localhost");
    private static final int    CASSANDRA_PORT   = AppConfig.getInt("cassandra.port",      9042);
    private static final long   BATCH_DURATION   = AppConfig.getInt("spark.batch.duration.ms", 20000);
    private static final String CHECKPOINT_DIR   = AppConfig.get("spark.checkpoint.dir",   "/tmp/spark-checkpoint-news");
    private static final String STOPWORDS_PATH   = AppConfig.get("stopwords.path",         "src/main/resources/stopwords.txt");
    private static final int    TOP_WORDS        = 10;

    private List<String> stopwords = Collections.emptyList();

    private String processBody(String body) {
        Map<String, Integer> freq = new HashMap<>();
        Stream.of(body.toLowerCase().split("[^a-zA-Z]"))
              .filter(w -> w.length() > 2 && !stopwords.contains(w))
              .forEach(w -> freq.merge(w, 1, Integer::sum));

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_WORDS)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
    }

    @Override
    public void run() {
        try {
            stopwords = Files.readAllLines(Paths.get(STOPWORDS_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Could not load stopwords from {}: {}", STOPWORDS_PATH, e.getMessage());
        }

        log.info("NewsStreamConsumer starting...");
        SparkConf conf = new SparkConf().setAppName("imdb-news-consumer").setMaster("local[*]");
        JavaStreamingContext ssc = new JavaStreamingContext(conf, new Duration(BATCH_DURATION));
        ssc.checkpoint(CHECKPOINT_DIR);

        Map<String, String> kafkaParams = new HashMap<>();
        kafkaParams.put("metadata.broker.list", BROKER);

        CassandraConnector connector = new CassandraConnector();
        connector.connectdb(CASSANDRA_HOST, CASSANDRA_PORT);

        final String insertQuery =
            "INSERT INTO imdb_keyspace2.newsTable (id, body, head, link, id_actor, publishTime, common_words) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement ps = connector.getSession().prepare(insertQuery);

        JavaPairInputDStream<String, String> stream = KafkaUtils.createDirectStream(
                ssc, String.class, String.class,
                StringDecoder.class, StringDecoder.class,
                kafkaParams, Collections.singleton(TOPIC));

        stream.foreachRDD(rdd -> {
            long count = rdd.count();
            if (count == 0) return;
            log.info("News batch: {} records across {} partitions", count, rdd.partitions().size());

            rdd.collect().forEach(record -> {
                try {
                    JSONObject json   = new JSONObject(record._2);
                    String id         = json.getString("id");
                    String body       = json.getString("body");
                    String head       = json.getString("head");
                    String link       = json.getString("link");
                    String idActor    = json.getString("id_actor");
                    String publishTime = json.getString("publishTime");
                    String commonWords = processBody(body);

                    BoundStatement bs = ps.bind(id, body, head, link, idActor, publishTime, commonWords);
                    connector.getSession().execute(bs);
                } catch (Exception e) {
                    log.error("Failed to process news record: {}", e.getMessage());
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
        new NewsStreamConsumer().run();
    }
}