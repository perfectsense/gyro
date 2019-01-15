package beam.test;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ResourceName("route-table")
public class RouteTableResource extends FakeResource {

    private String networkId;
    private List<String> subnetIds;
    private String routeTableId;
    private String ownerId;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getSubnetIds() {
        if (subnetIds == null) {
            subnetIds = new ArrayList<>();
        }

        return subnetIds;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public String getRouteTableId() {
        return routeTableId;
    }

    public void setRouteTableId(String routeTableId) {
        this.routeTableId = routeTableId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public void create() {
        setRouteTableId("route-" + UUID.randomUUID().toString().replace("-", "").substring(16));
        setOwnerId("owner-4a4f49a0b9fe");
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String routeTableId = getRouteTableId();

        if (routeTableId != null) {
            sb.append(routeTableId);

        } else {
            sb.append("fake route table");
        }

        return sb.toString();
    }

}
