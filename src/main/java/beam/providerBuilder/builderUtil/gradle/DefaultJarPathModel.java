package beam.providerBuilder.builderUtil.gradle;

import java.io.Serializable;

public class DefaultJarPathModel implements Serializable, JarPathModel {
    private final String name;
    private final String version;

    public DefaultJarPathModel(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @Override
    public String getJarPath() {
        return String.format("%s-%s-all.jar", name, version);
    }
}
