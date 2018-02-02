package beam.providerHandler;

import java.util.regex.Pattern;

public class MavenHandler extends ProviderHandler {

    private static String MAVEN_KEY = "^(?<organization>[^:]+):(?<package>[^:]+):(?<version>[^:]+)";
    private static Pattern MAVEN_KEY_PAT = Pattern.compile(MAVEN_KEY);

    @Override
    public boolean validate(String key) {
        return MAVEN_KEY_PAT.matcher(key).find();
    }

    @Override
    public void handle(String key) {

    }
}
