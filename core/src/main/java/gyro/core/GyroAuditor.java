package gyro.core;

import java.util.Map;

public interface GyroAuditor {

    void start(Map<String, Object> log) throws Exception;

    void append(String output) throws Exception;

    void finish(Map<String, Object> log, boolean success) throws Exception;

}
