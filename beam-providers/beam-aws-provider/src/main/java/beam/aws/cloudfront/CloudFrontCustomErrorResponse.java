package beam.aws.cloudfront;

import beam.core.diff.Diffable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CloudFrontCustomErrorResponse extends Diffable {

    private long ttl;
    private Integer errorCode;
    private Integer responseCode;
    private String responsePagePath;
    private Boolean customizeErrorResponse;

    private static final Set<Integer> ERROR_CODE_LIST = new HashSet<>(Arrays.asList(400,403,404,405,414,416,500,501,502,503,504));

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponsePagePath() {
        return responsePagePath;
    }

    public void setResponsePagePath(String responsePagePath) {
        this.responsePagePath = responsePagePath;
    }

    public Boolean getCustomizeErrorResponse() {
        return customizeErrorResponse;
    }

    public void setCustomizeErrorResponse(Boolean customizeErrorResponse) {
        this.customizeErrorResponse = customizeErrorResponse;
    }

    @Override
    public String primaryKey() {
        return getErrorCode() != null ? getErrorCode().toString() : "";
    }

    @Override
    public String toDisplayString() {
        return String.format("error response - code: %d, ttl: %d", getErrorCode(), getTtl());
    }
}
