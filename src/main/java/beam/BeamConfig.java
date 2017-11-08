package beam;

import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

public class BeamConfig {

    private static final Lazy<Map<String, Object>> CONFIG = new Lazy<Map<String, Object>>() {

        @Override
        @SuppressWarnings("unchecked")
        protected Map<String, Object> create() throws Exception {
            File globalConfigFile = Paths.get(BeamConfig.getBeamUserHome(), ".beam", "config.yml").toFile();

            if (globalConfigFile.exists()) {
                try (InputStream globalConfigInput = new FileInputStream(globalConfigFile)) {
                    return (Map<String, Object>) new Yaml().load(globalConfigInput);
                }

            } else {
                return new CompactMap<>();
            }
        }
    };

    public static <T> T get(Class<T> returnClass, String keyPath, T defaultValue) {
        T value = ObjectUtils.to(returnClass, CollectionUtils.getByPath(CONFIG.get(), keyPath));

        if (ObjectUtils.isBlank(value)) {
            return defaultValue;

        } else {
            return value;
        }
    }

    public static String getLogin() {
        return get(String.class, "enterpriseUser", null) != null ? get(String.class, "enterpriseUser", null) :
                get(String.class, "login", System.getProperty("user.name"));
    }

    public static String getBeamUserHome() {
        String userHome = System.getenv("BEAM_USER_HOME");
        if (ObjectUtils.isBlank(userHome)) {
            userHome = System.getProperty("user.home");
        }

        return userHome;
    }

}
