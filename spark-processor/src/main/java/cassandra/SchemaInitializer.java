package cassandra;

public class SchemaInitializer {
    public static void main(String[] args) {
        new KeyspaceCreator();
        new TableCreator();
        new CsvDataLoader();
    }
}