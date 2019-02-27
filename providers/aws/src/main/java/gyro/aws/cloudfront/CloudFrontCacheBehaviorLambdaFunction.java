package gyro.aws.cloudfront;

import gyro.core.diff.Diffable;
import gyro.core.diff.ResourceDiffProperty;
import software.amazon.awssdk.services.cloudfront.model.LambdaFunctionAssociation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CloudFrontCacheBehaviorLambdaFunction extends Diffable {

    private String eventType;
    private String arn;
    private Boolean includeBody;

    private static final Set<String> EventType =
        new HashSet<>(Arrays.asList("viewer-request", "viewer-response", "origin-request", "origin-response"));

    @ResourceDiffProperty(updatable = true)
    public String getEventType() {
        if (eventType == null) {
            eventType = "";
        }

        return eventType.toLowerCase();
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @ResourceDiffProperty(updatable = true)
    public String getArn() {
        if (arn == null) {
            arn = "";
        }

        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getIncludeBody() {
        if (includeBody == null) {
            includeBody = false;
        }

        return includeBody;
    }

    public void setIncludeBody(Boolean includeBody) {
        this.includeBody = includeBody;
    }

    public LambdaFunctionAssociation toLambdaFunctionAssociation() {
        return LambdaFunctionAssociation.builder()
            .eventType(getEventType())
            .includeBody(getIncludeBody())
            .lambdaFunctionARN(getArn())
            .build();
    }

    @Override
    public String primaryKey() {
        return getEventType();
    }

    @Override
    public String toDisplayString() {
        return "Lambda Function Association " + getEventType();
    }
}
