import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerApplication {

    private static final Logger log = LoggerFactory.getLogger(ProducerApplication.class);

    public static void main(String[] args) {
        NewsProducer producer = new NewsProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received — stopping producer...");
            producer.shutdown();
        }, "shutdown-hook"));

        Thread thread = new Thread(producer, "news-producer");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}