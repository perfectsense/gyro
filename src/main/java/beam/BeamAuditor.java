package beam;

import java.util.List;
import java.util.Map;

public interface BeamAuditor {

    void start(Map<String, Object> log) throws Exception;

    void append(String output) throws Exception;

    void finish(Map<String, Object> log, boolean success) throws Exception;

    List<Map<String, Object>> list(String accountName, String projectName) throws Exception;
}
