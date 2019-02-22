package beam.aws.rds;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DbClusterEndpointNotFoundException;
import software.amazon.awssdk.services.rds.model.DbClusterNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbClusterEndpointsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Create a db cluster endpoint.
 *
 * .. code-block:: beam
 *
 *    aws::db-cluster-endpoint endpoint-example
 *        cluster-endpoint-identifier: "endpoint"
 *        db-cluster-identifier: $(aws::db-cluster db-cluster-example | db-cluster-identifier)
 *        endpoint-type: "READER"
 *        static-members: [$(aws::db-instance db-instance-example | db-instance-identifier)]
 *    end
 */
@ResourceName("db-cluster-endpoint")
public class DbClusterEndpointResource extends AwsResource {

    private String clusterEndpointIdentifier;
    private String dbClusterIdentifier;
    private String endpointType;
    private List<String> excludedMembers;
    private List<String> staticMembers;

    /**
     * The identifier to use for the new endpoint. This parameter is stored as a lowercase string. (Required)
     */
    public String getClusterEndpointIdentifier() {
        return clusterEndpointIdentifier;
    }

    public void setClusterEndpointIdentifier(String clusterEndpointIdentifier) {
        this.clusterEndpointIdentifier = clusterEndpointIdentifier;
    }

    /**
     * The DB cluster identifier of the DB cluster associated with the endpoint. This parameter is stored as a lowercase string. (Required)
     */
    public String getDbClusterIdentifier() {
        return dbClusterIdentifier;
    }

    public void setDbClusterIdentifier(String dbClusterIdentifier) {
        this.dbClusterIdentifier = dbClusterIdentifier;
    }

    /**
     * The type of the endpoint. Valid values: ``READER``, ``ANY``. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    /**
     * List of DB instance identifiers that aren't part of the custom endpoint group. All other eligible instances are reachable through the custom endpoint. Only relevant if the list of static members is empty.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getExcludedMembers() {
        if (excludedMembers == null) {
            excludedMembers = new ArrayList<>();
        }

        return excludedMembers;
    }

    public void setExcludedMembers(List<String> excludedMembers) {
        this.excludedMembers = excludedMembers;
    }

    /**
     * List of DB instance identifiers that are part of the custom endpoint group.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getStaticMembers() {
        if (staticMembers == null) {
            staticMembers = new ArrayList<>();
        }

        return staticMembers;
    }

    public void setStaticMembers(List<String> staticMembers) {
        this.staticMembers = staticMembers;
    }

    @Override
    public boolean refresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getClusterEndpointIdentifier()) || ObjectUtils.isBlank(getDbClusterIdentifier())) {
            throw new BeamException("cluster-endpoint-identifier or db-cluster-identifier is missing, unable to load db cluster endpoint.");
        }

        try {
            DescribeDbClusterEndpointsResponse response = client.describeDBClusterEndpoints(
                r -> r.dbClusterEndpointIdentifier(getClusterEndpointIdentifier())
                        .dbClusterIdentifier(getDbClusterIdentifier())
            );

            response.dbClusterEndpoints().stream()
                .forEach(e -> {
                    setEndpointType(e.customEndpointType());
                    setExcludedMembers(e.excludedMembers());
                    setStaticMembers(e.staticMembers());
                }
            );

        } catch (DbClusterNotFoundException | DbClusterEndpointNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    public void create() {
        RdsClient client = createClient(RdsClient.class);
        client.createDBClusterEndpoint(
            r -> r.dbClusterEndpointIdentifier(getClusterEndpointIdentifier())
                    .dbClusterIdentifier(getDbClusterIdentifier())
                    .endpointType(getEndpointType())
                    .excludedMembers(getExcludedMembers())
                    .staticMembers(getStaticMembers())
        );
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        client.modifyDBClusterEndpoint(
            r -> r.dbClusterEndpointIdentifier(getClusterEndpointIdentifier())
                    .endpointType(getEndpointType())
                    .excludedMembers(getExcludedMembers())
                    .staticMembers(getStaticMembers())
        );
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteDBClusterEndpoint(
            r -> r.dbClusterEndpointIdentifier(getClusterEndpointIdentifier())
        );
    }

    @Override
    public String toDisplayString() {
        return "db cluster endpoint " + getClusterEndpointIdentifier();
    }
}
