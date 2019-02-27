package gyro.aws.ec2;

import gyro.aws.AwsResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import gyro.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.ConnectionNotification;
import software.amazon.awssdk.services.ec2.model.CreateVpcEndpointConnectionNotificationResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointConnectionNotificationsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates a connection notification for an endpoint or an endpoint service.
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     aws::connection-notification connection-notification-example
 *         vpc-endpoint-id: $(aws::endpoint endpoint-example-interface | endpoint-id)
 *         connection-notification-arn: "arn:aws:sns:us-west-2:242040583208:gyro-instance-state"
 *         connection-events: [
 *             "Accept"
 *         ]
 *     end
 *
 */
@ResourceName("connection-notification")
public class ConnectionNotificationResource extends AwsResource {

    private String serviceId;
    private String vpcEndpointId;
    private String connectionNotificationArn;
    private List<String> connectionEvents;
    private String connectionNotificationId;
    private String connectionNotificationState;
    private String connectionNotificationType;

    private final Set<String> masterEventSet = new HashSet<>(Arrays.asList(
        "Accept",
        "Connect",
        "Delete"
    ));

    /**
     * The id of the endpoint service. Either endpoint id or endpoint service id is required.
     */
    @ResourceOutput
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * The id of the vpc endpoint. Either endpoint id or endpoint service id is required.
     */
    public String getVpcEndpointId() {
        return vpcEndpointId;
    }

    public void setVpcEndpointId(String vpcEndpointId) {
        this.vpcEndpointId = vpcEndpointId;
    }

    /**
     * The ARN of the SNS topic. (Required)
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getConnectionNotificationArn() {
        return connectionNotificationArn;
    }

    public void setConnectionNotificationArn(String connectionNotificationArn) {
        this.connectionNotificationArn = connectionNotificationArn;
    }

    /**
     * The events this notification is subscribing to. Defaults to all values. Valid values [ 'Accept', 'Connect', 'Delete' ] (Required)
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getConnectionEvents() {
        if (connectionEvents == null) {
            connectionEvents = new ArrayList<>(masterEventSet);
        }

        return connectionEvents;
    }

    public void setConnectionEvents(List<String> connectionEvents) {
        this.connectionEvents = connectionEvents;
    }

    public String getConnectionNotificationId() {
        return connectionNotificationId;
    }

    public void setConnectionNotificationId(String connectionNotificationId) {
        this.connectionNotificationId = connectionNotificationId;
    }

    public String getConnectionNotificationState() {
        return connectionNotificationState;
    }

    public void setConnectionNotificationState(String connectionNotificationState) {
        this.connectionNotificationState = connectionNotificationState;
    }

    public String getConnectionNotificationType() {
        return connectionNotificationType;
    }

    public void setConnectionNotificationType(String connectionNotificationType) {
        this.connectionNotificationType = connectionNotificationType;
    }

    @Override
    public boolean refresh() {
        Ec2Client client = createClient(Ec2Client.class);

        ConnectionNotification connectionNotification = getConnectionNotification(client);

        if (connectionNotification == null) {
            return false;
        }

        setConnectionEvents(connectionNotification.connectionEvents());
        setConnectionNotificationArn(connectionNotification.connectionNotificationArn());
        setConnectionNotificationId(connectionNotification.connectionNotificationId());
        setConnectionNotificationState(connectionNotification.connectionNotificationStateAsString());
        setConnectionNotificationType(connectionNotification.connectionNotificationTypeAsString());
        setVpcEndpointId(connectionNotification.vpcEndpointId());
        setServiceId(connectionNotification.serviceId());

        return true;
    }

    @Override
    public void create() {
        Ec2Client client = createClient(Ec2Client.class);

        validate();

        CreateVpcEndpointConnectionNotificationResponse response = null;

        if (!ObjectUtils.isBlank(getVpcEndpointId())) {
            response = client.createVpcEndpointConnectionNotification(
                r -> r.vpcEndpointId(getVpcEndpointId())
                    .connectionEvents(getConnectionEvents())
                    .connectionNotificationArn(getConnectionNotificationArn())
            );
        } else if (!ObjectUtils.isBlank(getServiceId())) {
            response = client.createVpcEndpointConnectionNotification(
                r -> r.serviceId(getServiceId())
                    .connectionEvents(getConnectionEvents())
                    .connectionNotificationArn(getConnectionNotificationArn())
            );
        } else {
            throw new BeamException("Neither endpoint id nor service id found.");
        }

        setConnectionNotificationId(response.connectionNotification().connectionNotificationId());
        setConnectionNotificationType(response.connectionNotification().connectionNotificationTypeAsString());
        setConnectionNotificationState(response.connectionNotification().connectionNotificationStateAsString());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        validate();

        client.modifyVpcEndpointConnectionNotification(
            r -> r.connectionNotificationId(getConnectionNotificationId())
                .connectionEvents(getConnectionEvents())
                .connectionNotificationArn(getConnectionNotificationArn())
        );
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteVpcEndpointConnectionNotifications(
            r -> r.connectionNotificationIds(Collections.singleton(getConnectionNotificationId()))
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("connection notification");

        if (!ObjectUtils.isBlank(getConnectionNotificationId())) {
            sb.append(" - ").append(getConnectionNotificationId());
        }

        return sb.toString();
    }

    private ConnectionNotification getConnectionNotification(Ec2Client client) {
        if (ObjectUtils.isBlank(getConnectionNotificationId())) {
            throw new BeamException("connection-notification-id is missing, unable to load connection notification.");
        }

        try {
            DescribeVpcEndpointConnectionNotificationsResponse response = client.describeVpcEndpointConnectionNotifications(
                r -> r.connectionNotificationId(getConnectionNotificationId())
            );

            if (response.connectionNotificationSet().isEmpty()) {
                return null;
            }

            return response.connectionNotificationSet().get(0);
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return null;
            }

            throw ex;
        }
    }

    private void validate() {
        if ((ObjectUtils.isBlank(getVpcEndpointId()) && ObjectUtils.isBlank(getServiceId()))
            || (!ObjectUtils.isBlank(getVpcEndpointId()) && !ObjectUtils.isBlank(getServiceId()))) {
            throw new BeamException("Either 'vpc-endpoint-id' or 'service-id' needs to be set. Not both at a time.");
        }

        if (getConnectionEvents().stream().anyMatch(o -> !masterEventSet.contains(o))) {
            throw new BeamException("The values - (" + String.join(" , ", getConnectionEvents())
                + ") is invalid for parameter 'connection-events'. Valid values [ '" + String.join("', '") + "' ].");
        }
    }
}
