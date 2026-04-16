package cassandra;

import config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyspaceCreator {

    private static final Logger log = LoggerFactory.getLogger(KeyspaceCreator.class);

    public KeyspaceCreator() {
        String host = AppConfig.get("cassandra.host", "localhost");
        int    port = AppConfig.getInt("cassandra.port", 9042);
        try (CassandraConnector connector = new CassandraConnector()) {
            connector.connectdb(host, port);
            connector.getSession().execute(
                "CREATE KEYSPACE IF NOT EXISTS imdb_keyspace2 WITH " +
                "replication = {'class':'SimpleStrategy','replication_factor':1}");
            log.info("Keyspace imdb_keyspace2 created (or already exists).");
        }
    }
}