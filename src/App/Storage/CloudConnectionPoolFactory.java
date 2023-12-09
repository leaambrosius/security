package App.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class CloudConnectionPoolFactory {
    private static final String INSTANCE_CONNECTION_NAME = "carbide-eye-404202:europe-west2:secure-chat";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "CB9]'3{zcd'nx=_Z";
    private static final String DB_NAME = "chat-history";

    private static DataSource dataSource; // Singleton instance

    public static DataSource ds() {
        if (dataSource == null) {
            // Create a new configuration and set the database credentials
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:postgresql:///%s", DB_NAME));
            config.setUsername(DB_USER);
            config.setPassword(DB_PASS);
            config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
            config.addDataSourceProperty("cloudSqlInstance", INSTANCE_CONNECTION_NAME);

            // Initialize the connection pool using the configuration object.
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }
}