package cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraConnector implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CassandraConnector.class);

    private Cluster cluster;
    private Session session;

    public void connectdb(String host, int port) {
        this.cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
        this.session = cluster.connect();
        log.info("Connected to Cassandra at {}:{}", host, port);
    }

    public Session getSession() {
        return session;
    }

    @Override
    public void close() {
        if (cluster != null && !cluster.isClosed()) {
            cluster.close();
            log.info("Cassandra connection closed.");
        }
    }
}