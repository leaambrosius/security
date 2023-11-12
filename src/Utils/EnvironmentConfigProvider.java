package Utils;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvironmentConfigProvider {
    private static Dotenv dotenv = null;

    public static synchronized Dotenv getInstance() {
        if (dotenv == null) {
            dotenv = Dotenv.configure().load();
        }
        return dotenv;
    }
}
