package beam.plugins.enterprise;

import beam.core.LocalFileBackend;
import beam.lang.Resource;
import beam.lang.ast.scope.RootScope;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnterpriseConfig {

    private static final Lazy<Map<String, Object>> CONFIG = new Lazy<Map<String, Object>>() {

        @Override
        @SuppressWarnings("unchecked")
        protected Map<String, Object> create() throws Exception {
            File enterpriseConfigFile = Paths.get(EnterpriseConfig.getUserHome(), ".beam", "enterprise.bcl").toFile();

            if (enterpriseConfigFile.exists()) {
                //core.addResourceType("enterprise::project", EnterpriseProject.class);

                RootScope config = new RootScope(enterpriseConfigFile.toString());

                new LocalFileBackend().load(config);

                Map<String, Object> keyValues = new HashMap<>(config);
                for (Resource resource : config.getFileScope().getResources().values()) {
                    keyValues.put(resource.resourceIdentifier(), resource);
                }

                return keyValues;
            } else {
                return new CompactMap<>();
            }
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> returnClass, String project, String keyPath, T defaultValue) {
        EnterpriseProject projectConfig = (EnterpriseProject) CONFIG.get().get(project);
        T value = projectConfig != null ? (T) projectConfig.resolvedKeyValues().get(keyPath) : null;

        if (value == null) {
            value = get(returnClass, keyPath, defaultValue);
        }

        return value;
    }

    public static <T> T get(Class<T> returnClass, String keyPath, T defaultValue) {
        T value = ObjectUtils.to(returnClass, CollectionUtils.getByPath(CONFIG.get(), keyPath));

        if (ObjectUtils.isBlank(value)) {
            return defaultValue;

        } else {
            return value;
        }
    }

    public static String getLogin() {
        return get(String.class, "enterprise-user", null) != null ? get(String.class, "enterprise-user", null) :
            get(String.class, "login", System.getProperty("user.name"));
    }

    public static String getUserHome() {
        String userHome = System.getenv("BEAM_USER_HOME");
        if (ObjectUtils.isBlank(userHome)) {
            userHome = System.getProperty("user.home");
        }

        return userHome;
    }

}
