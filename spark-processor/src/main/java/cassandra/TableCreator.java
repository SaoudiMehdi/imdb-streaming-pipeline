package cassandra;

import config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableCreator {

    private static final Logger log = LoggerFactory.getLogger(TableCreator.class);

    public TableCreator() {
        String host = AppConfig.get("cassandra.host", "localhost");
        int    port = AppConfig.getInt("cassandra.port", 9042);
        try (CassandraConnector connector = new CassandraConnector()) {
            connector.connectdb(host, port);

            connector.getSession().execute(
                "CREATE TABLE IF NOT EXISTS imdb_keyspace2.movies " +
                "(idMovie text, rating text, title text, releaseDate text, runningTimeInMinutes text, " +
                "PRIMARY KEY (idMovie))");

            connector.getSession().execute(
                "CREATE TABLE IF NOT EXISTS imdb_keyspace2.topRatedMovies " +
                "(idMovie text, ranking text, PRIMARY KEY (idMovie))");

            connector.getSession().execute(
                "CREATE TABLE IF NOT EXISTS imdb_keyspace2.actors " +
                "(idActor text, name text, birthDate text, birthPlace text, gender text, " +
                "PRIMARY KEY (idActor))");

            connector.getSession().execute(
                "CREATE TABLE IF NOT EXISTS imdb_keyspace2.actorMovies " +
                "(idActor text, idMovie text, PRIMARY KEY (idActor, idMovie))");

            connector.getSession().execute(
                "CREATE TABLE IF NOT EXISTS imdb_keyspace2.newsTable " +
                "(id text, body text, head text, link text, id_actor text, publishTime text, common_words text, " +
                "PRIMARY KEY (id))");

            log.info("All tables created (or already exist).");
        }
    }
}