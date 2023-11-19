package Utils;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvironmentConfigProvider {
    private static Dotenv dotenv = null;

    private static synchronized Dotenv getInstance() {
        if (dotenv == null) {
            dotenv = Dotenv.configure().load();
        }
        return dotenv;
    }

    public static String getServerIP() {
        return getInstance().get("SERVER_ADDRESS");
    }

    public static String getServerPort() {
        return getInstance().get("SERVER_PORT");
    }

    public static String getPeerDefaultPort() {
        return getInstance().get("PEER_DEFAULT_PORT");
    }
}
