package cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.opencsv.CSVReader;
import config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.List;

public class CsvDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CsvDataLoader.class);

    private final String ALL_KNOWN_FOR_PATH = AppConfig.get("csv.actorMovies", "src/main/resources/movie_actor/actorKnownFor.csv");
    private final String TOP_RATED_PATH     = AppConfig.get("csv.topRated",    "src/main/resources/movie/topRatedMovies.csv");
    private final String MOVIES_PATH        = AppConfig.get("csv.movies",      "src/main/resources/movie/knownMovies.csv");
    private final String MOST_POPULAR_PATH  = AppConfig.get("csv.actors",      "src/main/resources/actor/mostPopularCelebs.csv");

    public CsvDataLoader() {
        try (CassandraConnector connector = new CassandraConnector()) {
            connector.connectdb(
                    AppConfig.get("cassandra.host", "localhost"),
                    AppConfig.getInt("cassandra.port", 9042));

            insertMovies(connector);
            insertTopRatedMovies(connector);
            insertActors(connector);
            insertActorMovieLinks(connector);
        }
    }

    public void insertMovies(CassandraConnector connector) {
        final String query = "INSERT INTO imdb_keyspace2.movies (idMovie, rating, title, releaseDate, runningTimeInMinutes) VALUES (?,?,?,?,?)";
        PreparedStatement ps = connector.getSession().prepare(query);
        try (CSVReader csv = new CSVReader(new FileReader(MOVIES_PATH))) {
            List<String[]> rows = csv.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                connector.getSession().execute(ps.bind(r[0], r[1], r[3], r[5], r[6]));
            }
            log.info("Inserted {} movies.", rows.size() - 1);
        } catch (Exception e) {
            log.error("insertMovies failed: {}", e.getMessage());
        }
    }

    public void insertTopRatedMovies(CassandraConnector connector) {
        final String query = "INSERT INTO imdb_keyspace2.topRatedMovies (idMovie, ranking) VALUES (?,?)";
        PreparedStatement ps = connector.getSession().prepare(query);
        try (CSVReader csv = new CSVReader(new FileReader(TOP_RATED_PATH))) {
            List<String[]> rows = csv.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                connector.getSession().execute(ps.bind(r[1], r[0]));
            }
            log.info("Inserted {} top-rated movies.", rows.size() - 1);
        } catch (Exception e) {
            log.error("insertTopRatedMovies failed: {}", e.getMessage());
        }
    }

    public void insertActors(CassandraConnector connector) {
        final String query = "INSERT INTO imdb_keyspace2.actors (idActor, name, birthDate, birthPlace, gender) VALUES (?,?,?,?,?)";
        PreparedStatement ps = connector.getSession().prepare(query);
        try (CSVReader csv = new CSVReader(new FileReader(MOST_POPULAR_PATH))) {
            List<String[]> rows = csv.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                connector.getSession().execute(ps.bind(r[0], r[1], r[2], r[3], r[4]));
            }
            log.info("Inserted {} actors.", rows.size() - 1);
        } catch (Exception e) {
            log.error("insertActors failed: {}", e.getMessage());
        }
    }

    public void insertActorMovieLinks(CassandraConnector connector) {
        final String query = "INSERT INTO imdb_keyspace2.actorMovies (idActor, idMovie) VALUES (?,?)";
        PreparedStatement ps = connector.getSession().prepare(query);
        try (CSVReader csv = new CSVReader(new FileReader(ALL_KNOWN_FOR_PATH))) {
            List<String[]> rows = csv.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                connector.getSession().execute(ps.bind(r[0], r[1]));
            }
            log.info("Inserted {} actor-movie links.", rows.size() - 1);
        } catch (Exception e) {
            log.error("insertActorMovieLinks failed: {}", e.getMessage());
        }
    }
}