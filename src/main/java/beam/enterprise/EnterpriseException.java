package beam.enterprise;

import com.psddev.dari.util.ObjectUtils;

import java.util.Map;

public class EnterpriseException extends RuntimeException {

    private final Map<String, Object> responseMap;

    public EnterpriseException(Map<String, Object> responseMap) {
        this.responseMap = responseMap;
    }

    @Override
    public String getMessage() {
        return ObjectUtils.toJson(responseMap);
    }
}
