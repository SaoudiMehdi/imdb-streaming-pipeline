import api.actor.ActorBiographyClient;
import api.actor.ActorBornTodayClient;
import api.actor.ActorNewsClient;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Actor;
import util.AppConfig;
import util.News;
import util.SimpleDate;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NewsProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(NewsProducer.class);

    private static final String TOPIC      = AppConfig.get("kafka.topic.news",      "NewsTopic");
    private static final String CSV_PATH   = AppConfig.get("csv.mostPopularCelebs", "src/main/resources/actor/mostPopularCelebs.csv");
    private static final int    MAX_ACTORS = AppConfig.getInt("actors.max", 6);

    private volatile boolean running = true;

    public void shutdown() {
        running = false;
    }

    public void addActorToFile(Actor actor) {
        try (FileWriter fw = new FileWriter(new File(CSV_PATH), true);
             CSVWriter writer = new CSVWriter(fw)) {
            writer.writeNext(actor.toArray());
            log.info("Wrote actor {} to CSV", actor.getId());
        } catch (IOException e) {
            log.error("Failed to write actor {} to CSV: {}", actor.getId(), e.getMessage());
        }
    }

    @Override
    public void run() {
        Producer<String, String> producer = ActorProducer.createProducer();
        List<String> actorIds = new ArrayList<>();
        Map<String, List<News>> newsMap = new HashMap<>();
        SimpleDate currentDate = new SimpleDate();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");

        try {
            while (running) {
                try {
                    SimpleDate today = new SimpleDate();
                    if (actorIds.isEmpty() || !today.equals(currentDate)) {
                        newsMap.clear();
                        actorIds.clear();
                        currentDate = today;
                        loadActorsFromCsv(actorIds, currentDate);
                        fillFromApiIfNeeded(actorIds, currentDate, producer);
                        buildNewsMap(actorIds, currentDate, newsMap);
                    }

                    String timeKey = formatter.format(new Date());
                    if (newsMap.containsKey(timeKey)) {
                        for (News news : newsMap.remove(timeKey)) {
                            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, news.getId(), news.jsonAsString());
                            producer.send(record, (meta, ex) -> {
                                if (ex != null) log.error("Failed to send news {}: {}", news.getId(), ex.getMessage());
                                else log.info("News sent → topic={} offset={}", meta.topic(), meta.offset());
                            });
                        }
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    log.error("CSV read error: {}", e.getMessage());
                }
            }
        } finally {
            producer.flush();
            producer.close();
            log.info("NewsProducer stopped cleanly.");
        }
    }

    private void loadActorsFromCsv(List<String> actorIds, SimpleDate date) throws IOException {
        try (CSVReader csvReader = new CSVReader(new FileReader(CSV_PATH))) {
            List<String[]> rows = csvReader.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 3) continue;
                String[] parts = row[2].split("-");
                if (parts.length < 3) continue;
                int month = Integer.parseInt(parts[1]);
                int day   = Integer.parseInt(parts[2]);
                if (date.sameMonthDay(month, day)) actorIds.add(row[0]);
            }
        }
    }

    private void fillFromApiIfNeeded(List<String> actorIds, SimpleDate date, Producer<String, String> producer) {
        if (actorIds.size() >= MAX_ACTORS) return;
        ActorBornTodayClient bornToday = new ActorBornTodayClient(date.getMonth(), date.getDay());
        for (String actorId : bornToday.getAllBornToday()) {
            if (actorIds.size() >= MAX_ACTORS) break;
            if (actorIds.contains(actorId)) continue;
            Actor actor = new ActorBiographyClient(actorId).getActor();
            if (actor != null) {
                new ActorProducer(actor, producer).run();
                addActorToFile(actor);
                actorIds.add(actorId);
            }
        }
    }

    private void buildNewsMap(List<String> actorIds, SimpleDate date, Map<String, List<News>> newsMap) {
        for (String actorId : actorIds) {
            ActorNewsClient newsClient = new ActorNewsClient(actorId);
            News news;
            while ((news = newsClient.getNextNews(date)) != null) {
                String timeKey = news.getPublishTime().split("T")[1].substring(0, 5);
                newsMap.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(news);
            }
        }
    }
}