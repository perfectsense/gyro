package beam.config;

import java.util.ArrayList;
import java.util.List;

public class AccessPermissionConfig extends Config {

    private String name;
    private String cidr;
    private String copy;
    private String protocol;
    private List<Integer> ports;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getCopy() {
        return copy;
    }

    public void setCopy(String copy) {
        this.copy = copy;
    }

    public String getProtocol() {
        return protocol != null ? protocol : "tcp";
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public List<Integer> getPorts() {
        if (ports == null) {
            ports = new ArrayList<>();
        }
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }
}
