package beam.aws.config;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceDiffProperty;
import beam.config.ConfigKey;
import beam.config.ConfigValue;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.psddev.dari.util.ObjectUtils;

public class HostedZoneRRSetResource extends AWSResource<ResourceRecordSet> {

    private BeamReference aliasTarget;
    private AliasTarget internalAliasTarget;
    private String resourceRegion;
    private BeamReference hostedZone;
    private String name;
    private Long ttl;
    private Long weight;
    private String type;
    private String setIdentifier;
    private Set<Value> values;
    private RoutingType routingType;

    public <T extends AmazonWebServiceClient> T createClient(Class<T> clientClass, AWSCredentialsProvider provider) {

        try {
            Constructor<?> constructor = clientClass.getConstructor(AWSCredentialsProvider.class);

            T client = (T) constructor.newInstance(provider);

            if (getRegion() != null) {
                client.setRegion(getRegion());
            }

            return client;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public BeamReference getAliasTarget() {
        return aliasTarget;
    }

    public void setAliasTarget(BeamReference aliasTarget) {
        this.aliasTarget = aliasTarget;
    }

    public String getResourceRegion() {
        return resourceRegion;
    }

    public void setResourceRegion(String resourceRegion) {
        this.resourceRegion = resourceRegion;
    }

    public BeamReference getHostedZone() {
        return newParentReference(HostedZoneResource.class, hostedZone);
    }

    public void setHostedZone(BeamReference hostedZone) {
        this.hostedZone = hostedZone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getTtl() {
        if (routingType == RoutingType.WEIGHTED) {
            return null;
        }

        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getWeight() {
        if (routingType == RoutingType.WEIGHTED && weight == null) {
            return 0L;
        }

        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    @ResourceDiffProperty(updatable = true)
    public String getType() {
        if (routingType == RoutingType.WEIGHTED) {
            return "A";
        }

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ResourceDiffProperty(updatable = true)
    public Set<Value> getValues() {
        if (values == null) {
            values = new NullSet<>();
        }
        return values;
    }

    public void setValues(Set<Value> values) {
        this.values = values;
    }

    @ResourceDiffProperty(updatable = true)
    public RoutingType getRoutingType() {
        return routingType;
    }

    public void setRoutingType(RoutingType routingType) {
        this.routingType = routingType;
    }

    public ResourceRecordSet toResourceRecordSet() {
        ResourceRecordSet rrs = new ResourceRecordSet();
        BeamReference aliasTarget = getAliasTarget();

        rrs.setName(getName());

        if (internalAliasTarget != null) {
            rrs.setAliasTarget(internalAliasTarget);
            rrs.setType(RRType.A);
        } else if (aliasTarget != null) {
            BeamResource aliasTargetResource = aliasTarget.resolve();

            if (aliasTargetResource instanceof LoadBalancerResource) {
                LoadBalancerResource lb = (LoadBalancerResource) aliasTargetResource;
                AliasTarget at = new AliasTarget();

                at.setDNSName(lb.getDnsName());
                at.setEvaluateTargetHealth(Boolean.FALSE);
                at.setHostedZoneId(lb.getCanonicalHostedZoneNameId());
                rrs.setAliasTarget(at);
                rrs.setType(RRType.A);

                if (getRoutingType() == RoutingType.WEIGHTED) {
                    setSetIdentifier(String.format("%s [%s] elb", lb.getRegion().getName(), lb.getLoadBalancerName()));
                }
            }
        } else {
            rrs.setTTL(getTtl());
            rrs.setType(getType());
            rrs.setSetIdentifier(getSetIdentifier());

            for (Value value : getValues()) {
                value.setType(getType());
                String valueString = value.resolve();

                if (valueString != null) {
                    rrs.getResourceRecords().add(new ResourceRecord(valueString));
                }
            }
        }

        if (getRoutingType() == RoutingType.WEIGHTED) {
            rrs.setWeight(getWeight());
            rrs.setSetIdentifier(getSetIdentifier());
        } else if (getRoutingType() == RoutingType.LATENCY) {
            rrs.setTTL(null);
            rrs.setType(RRType.A);
            rrs.setRegion(getResourceRegion());
            rrs.setSetIdentifier(getSetIdentifier());
        }

        return rrs;
    }

    public String getSetIdentifier() {
        if (setIdentifier == null && getAliasTarget() != null) {
            BeamResource aliasTargetResource = getAliasTarget().resolve();

            if (aliasTargetResource instanceof LoadBalancerResource) {
                LoadBalancerResource lb = (LoadBalancerResource) aliasTargetResource;

                if (getRoutingType() == RoutingType.WEIGHTED) {
                    setSetIdentifier(String.format("%s [%s] elb", lb.getRegion().getName(), lb.getLoadBalancerName()));
                }
            }
        }

        return setIdentifier;
    }

    public void setSetIdentifier(String setIdentifier) {
        this.setIdentifier = setIdentifier;
    }

    @Override
    public List<Object> diffIds() {
        return Arrays.asList(getHostedZone(), getName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, ResourceRecordSet rrs) {
        setName(rrs.getName().replace("\\052", "*"));
        setTtl(rrs.getTTL());
        setType(rrs.getType());
        setSetIdentifier(rrs.getSetIdentifier());
        this.internalAliasTarget = rrs.getAliasTarget();

        if (rrs.getWeight() != null) {
            setRoutingType(RoutingType.WEIGHTED);
            setWeight(rrs.getWeight());
        } else if (rrs.getRegion() != null) {
            setResourceRegion(rrs.getRegion());
            setRoutingType(RoutingType.LATENCY);
        }

        for (ResourceRecord rr : rrs.getResourceRecords()) {
            getValues().add(new StringValue(rr.getValue()));
        }
    }

    @Override
    public void create(AWSCloud cloud) {
        change(cloud, new Change(ChangeAction.CREATE, toResourceRecordSet()));
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, ResourceRecordSet> current, Set<String> changedProperties) {
        HostedZoneRRSetResource currentRrs = (HostedZoneRRSetResource) current;

        change(
                cloud,
                new Change(ChangeAction.DELETE, currentRrs.toResourceRecordSet()),
                new Change(ChangeAction.CREATE, toResourceRecordSet())
        );
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AWSCloud cloud) {
        throw new UnsupportedOperationException();
    }

    private void change(AWSCloud cloud, Change... changes) {
        AmazonRoute53Client client = createClient(AmazonRoute53Client.class, cloud.getProvider());
        ChangeResourceRecordSetsRequest crrsRequest = new ChangeResourceRecordSetsRequest();

        crrsRequest.setChangeBatch(new ChangeBatch(Arrays.asList(changes)));
        crrsRequest.setHostedZoneId(getHostedZone().awsId());
        client.changeResourceRecordSets(crrsRequest);
    }

    @Override
    public String toDisplayString() {
        return "RR set '" + getName() + "' " + getType();
    }

    @ConfigKey("type")
    public static abstract class Value {

        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public abstract String resolve();

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            String value = resolve();

            if (value == null && this instanceof ReferenceValue) {
               ReferenceValue referenceValue = (ReferenceValue) this;
               value = referenceValue.getReference().toString();
            }

            return value;
        }
    }

    @ConfigValue("string")
    public static class StringValue extends Value {

        private String string;

        public StringValue(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        @Override
        public String resolve() {
            return getString();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof Value) {
                return ObjectUtils.equals(
                        resolve(),
                        ((Value) other).resolve());

            } else {
                return false;
            }
        }
    }

    @ConfigValue("reference")
    public static class ReferenceValue extends Value {

        private BeamReference reference;

        private boolean resolvePrivateIp = false;

        public ReferenceValue(BeamReference reference) {
            this.reference = reference;
        }

        public ReferenceValue(BeamReference reference, boolean resolvePrivateIp) {
            this.reference = reference;
            this.resolvePrivateIp = resolvePrivateIp;
        }

        public BeamReference getReference() {
            return reference;
        }

        public void setReference(BeamReference reference) {
            this.reference = reference;
        }
        @Override
        public String resolve() {
            BeamReference reference = getReference();

            if (reference != null) {
                BeamResource resource = reference.resolve();

                if (resource instanceof InstanceResource) {
                    if ("CNAME".equals(getType())) {
                        return ((InstanceResource) resource).getPublicDnsName();
                    } else if (!resolvePrivateIp) {
                        if (((InstanceResource) resource).getPublicIpAddress() != null) {
                            return ((InstanceResource) resource).getPublicIpAddress();

                        } else {
                            return reference.toString();
                        }
                    } else {
                        return ((InstanceResource) resource).getPrivateIpAddress();
                    }

                } else if (resource instanceof LoadBalancerResource) {
                    return ((LoadBalancerResource) resource).getDnsName();
                }
            }

            return null;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof ReferenceValue) {
                return ObjectUtils.equals(
                        getReference(),
                        ((ReferenceValue) other).getReference());

            } else if (other instanceof Value) {
                return ObjectUtils.equals(
                        resolve(),
                        ((Value) other).resolve());

            } else {
                return false;
            }
        }
    }

    public static enum RoutingType {
        WEIGHTED("weighted"),
        LATENCY("latency"),
        SIMPLE("simple");

        private String name;

        RoutingType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static RoutingType findRouteType(String type) {
            if (type != null) {
                type = type.toUpperCase();
            }

            for (RoutingType rt : values()) {
                if (rt.toString().equals(type)) {
                    return rt;
                }
            }

            return null;
        }
    }
}
