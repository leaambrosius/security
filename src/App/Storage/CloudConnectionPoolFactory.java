package App.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class CloudConnectionPoolFactory {
    private static final String INSTANCE_CONNECTION_NAME = "carbide-eye-404202:europe-west2:secure-chat";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "CB9]'3{zcd'nx=_Z";
    private static final String DB_NAME = "chat-history";

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        config.setJdbcUrl(String.format("jdbc:postgresql:///%s", DB_NAME));
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
        config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
        config.addDataSourceProperty("cloudSqlInstance", INSTANCE_CONNECTION_NAME);
        ds = new HikariDataSource( config );
    }

    private CloudConnectionPoolFactory() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}

