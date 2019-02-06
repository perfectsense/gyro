package beam.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

@ResourceName("listener")
public class ListenerResource extends FakeResource {

    private String listenerArn;
    private Integer port;
    private String protocol;
    private List<ListenerAction> action;

    public String getListenerArn() {
        return listenerArn;
    }

    public void setListenerArn(String listenerArn) {
        this.listenerArn = listenerArn;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @ResourceDiffProperty(updatable = true)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ResourceDiffProperty(nullable = true)
    public List<ListenerAction> getAction() {
        if (action == null) {
            action = new ArrayList<>();
        }

        return action;
    }

    public void setAction(List<ListenerAction> action) {
        this.action = action;
    }

    @Override
    public void create() {
        setListenerArn("arn:" + UUID.randomUUID().toString());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("fake alb listener - ");
        sb.append(getProtocol());
        sb.append(" ");
        sb.append(getPort());

        return sb.toString();
    }

}