package beam.aws.config;

import java.util.Map;

public interface Taggable {

    public Map<String, String> getTags();
}
