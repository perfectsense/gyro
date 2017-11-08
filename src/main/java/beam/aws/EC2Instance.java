package beam.aws;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

import beam.BeamInstance;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceAttributeName;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.io.BaseEncoding;
import com.psddev.dari.util.ObjectUtils;

public class EC2Instance extends BeamInstance {

    private final AWSCloud cloud;
    private final Instance instance;

    private String instanceId;
    private String environment;
    private String location;
    private String region;
    private String state;
    private String layer;
    private String publicIpAddress;
    private String privateIpAddress;
    private Date launchTime;
    private Boolean sandboxed;
    private Map<String, String> tags;

    public EC2Instance(AWSCloud cloud, Instance instance) {
        this.cloud = cloud;
        this.instance = instance;

        instanceId = instance.getInstanceId();
        environment = AWSUtils.getTagValue(instance.getTags(), "beam.env");
        location = instance.getPlacement().getAvailabilityZone();
        region = location.substring(0, location.length() - 1);
        layer = AWSUtils.getTagValue(instance.getTags(), "beam.layer");
        state = instance.getState().getName();
        publicIpAddress = instance.getPublicIpAddress();
        privateIpAddress = instance.getPrivateIpAddress();
        launchTime = instance.getLaunchTime();

        for (Tag tag : instance.getTags()) {
            getTags().put(tag.getKey(), tag.getValue());
        }

        isSandboxed();
    }

    public EC2Instance(AWSCloud cloud, Map<String, Object> instanceMap) {
        this.cloud = cloud;
        this.instance = null;

        instanceId = ObjectUtils.to(String.class, instanceMap.get("serverId"));
        environment = ObjectUtils.to(String.class, instanceMap.get("environment"));
        location = ObjectUtils.to(String.class, instanceMap.get("location"));
        region = ObjectUtils.to(String.class, instanceMap.get("region"));
        layer = ObjectUtils.to(String.class, instanceMap.get("layer"));
        state = ObjectUtils.to(String.class, instanceMap.get("serverState"));
        publicIpAddress = ObjectUtils.to(String.class, instanceMap.get("publicIp"));
        privateIpAddress = ObjectUtils.to(String.class, instanceMap.get("privateIp"));
        sandboxed = ObjectUtils.to(Boolean.class, instanceMap.get("sandbox"));
        launchTime = new Date(ObjectUtils.to(Long.class, instanceMap.get("date")));
        tags = (Map<String, String>) instanceMap.get("tags");
    }

    public void updateClientRegion(AmazonWebServiceClient client) {
        Region region = Region.getRegion(Regions.valueOf(getRegion().replace('-', '_').toUpperCase(Locale.ENGLISH)));

        client.setRegion(region);
    }

    @Override
    public String getId() {
        return instanceId;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getLayer() {
        return layer;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public boolean isSandboxed() {
        if (sandboxed == null) {
            AmazonEC2Client client = new AmazonEC2Client(cloud.getProvider());
            updateClientRegion(client);

            DescribeInstanceAttributeRequest attributeRequest = new DescribeInstanceAttributeRequest();
            attributeRequest.withAttribute(InstanceAttributeName.UserData);
            attributeRequest.withInstanceId(getId());

            DescribeInstanceAttributeResult attributeResult = client.describeInstanceAttribute(attributeRequest);
            if (attributeResult.getInstanceAttribute().getUserData() != null) {
                String userdataJson = new String(BaseEncoding.base64().decode(attributeResult.getInstanceAttribute().getUserData()));
                Map<String, String> userdata = (Map<String, String>) ObjectUtils.fromJson(userdataJson);

                if ("true".equals(userdata.get("sandbox"))) {
                    sandboxed = true;
                } else {
                    sandboxed = false;
                }
            }
        }

        return sandboxed;
    }

    @Override
    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    @Override
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    @Override
    public Date getDate() {
        return launchTime;
    }

    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }

}