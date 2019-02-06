package beam.test;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;

public class ListenerActionResource extends Diffable {

    private Integer order;
    private String targetGroupArn;
    private String type;
    private AuthenticateCognitoConfig cognito;

    @ResourceDiffProperty(updatable = true)
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getTargetGroupArn() {
        return targetGroupArn;
    }

    public void setTargetGroupArn(String targetGroupArn) {
        this.targetGroupArn = targetGroupArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ResourceDiffProperty(nullable = true)
    public AuthenticateCognitoConfig getCognito() {
        return cognito;
    }

    public void setCognito(AuthenticateCognitoConfig cognito) {
        this.cognito = cognito;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("action ");
        sb.append(getType());

        return sb.toString();
    }

}
