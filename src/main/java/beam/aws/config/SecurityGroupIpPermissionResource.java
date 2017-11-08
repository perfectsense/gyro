package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import com.google.common.base.Joiner;
import com.psddev.dari.util.ObjectUtils;

public class SecurityGroupIpPermissionResource extends AWSResource<IpPermission> {

    private BeamReference fromGroup;
    private Integer fromPort;
    private String ipProtocol;
    private String ipRange;
    private Integer toPort;

    @ResourceDiffProperty(updatable = true)
    public BeamReference getFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(BeamReference fromGroup) {
        this.fromGroup = fromGroup;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getFromPort() {
        return fromPort;
    }

    public void setFromPort(Integer fromPort) {
        this.fromPort = fromPort;
    }

    @ResourceDiffProperty(updatable = true)
    public String getIpProtocol() {
        return ipProtocol;
    }

    public void setIpProtocol(String ipProtocol) {
        this.ipProtocol = ipProtocol;
    }

    @ResourceDiffProperty(updatable = true)
    public String getIpRange() {
        return ipRange;
    }

    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getToPort() {
        return toPort;
    }

    public void setToPort(Integer toPort) {
        this.toPort = toPort;
    }

    public IpPermission toIpPermission() {
        IpPermission perm = new IpPermission();

        perm.setFromPort(getFromPort());
        perm.setIpProtocol(getIpProtocol());

        String ipRange = getIpRange();

        if (ipRange != null) {
            perm.setIpRanges(Arrays.asList(ipRange));
        }

        BeamReference fromGroup = getFromGroup();

        if (fromGroup != null) {
            UserIdGroupPair pair = new UserIdGroupPair();

            perm.getUserIdGroupPairs().add(pair);
            pair.setGroupId(fromGroup.awsId());
        }

        perm.setToPort(getToPort());
        return perm;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(Joiner.
                on(',').
                useForNull("null").
                join(Arrays.asList(
                        getFromGroup(),
                        getFromPort(),
                        getIpProtocol(),
                        getIpRange(),
                        getToPort())));
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, IpPermission perm) {
        setFromPort(perm.getFromPort());
        setIpProtocol(perm.getIpProtocol());
        setToPort(perm.getToPort());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        AuthorizeSecurityGroupIngressRequest asgiRequest = new AuthorizeSecurityGroupIngressRequest();

        asgiRequest.setGroupId(findParent(SecurityGroupResource.class).getGroupId());
        asgiRequest.getIpPermissions().add(toIpPermission());
        client.authorizeSecurityGroupIngress(asgiRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, IpPermission> current, Set<String> changedProperties) {
        current.delete(cloud);
        create(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        RevokeSecurityGroupIngressRequest rsgiRequest = new RevokeSecurityGroupIngressRequest();

        rsgiRequest.setGroupId(findParent(SecurityGroupResource.class).getGroupId());
        rsgiRequest.getIpPermissions().add(toIpPermission());
        client.revokeSecurityGroupIngress(rsgiRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("IP permission - allow ");

        String ipProtocol = getIpProtocol();

        if ("-1".equals(ipProtocol)) {
            sb.append("all traffic");

        } else {
            sb.append(ipProtocol);
            sb.append(" traffic");

            Integer fromPort = getFromPort();
            Integer toPort = getToPort();

            if (ObjectUtils.equals(fromPort, toPort)) {
                sb.append(" at ");
                sb.append(fromPort);

            } else {
                sb.append(" between ");
                sb.append(fromPort);
                sb.append(" and ");
                sb.append(toPort);
            }
        }

        BeamReference fromGroup = getFromGroup();

        if (fromGroup != null) {
            sb.append(" from ");
            sb.append(fromGroup);

        } else {
            String ipRange = getIpRange();

            if ("0.0.0.0/0".equals(ipRange)) {
                sb.append(" from everywhere");

            } else {
                sb.append(" from ");
                sb.append(ipRange);
            }
        }

        return sb.toString();
    }
}
