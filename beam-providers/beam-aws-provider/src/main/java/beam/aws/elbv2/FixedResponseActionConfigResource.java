package beam.aws.elbv2;

import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.FixedResponseActionConfig;

@ResourceName(parent = "action", value = "fixed-response")
public class FixedResponseActionConfigResource {

    private String contentType;
    private String messageBody;
    private String statusCode;

    public FixedResponseActionConfigResource() {

    }

    public FixedResponseActionConfigResource(FixedResponseActionConfig fixed) {
        setContentType(fixed.contentType());
        setMessageBody(fixed.messageBody());
        setStatusCode(fixed.statusCode());
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public FixedResponseActionConfig toFixedAction() {
        return FixedResponseActionConfig.builder()
                .contentType(getContentType())
                .messageBody(getMessageBody())
                .statusCode(getStatusCode())
                .build();
    }
}
