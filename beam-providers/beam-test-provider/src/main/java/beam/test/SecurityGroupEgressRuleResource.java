package beam.test;

import beam.core.diff.ResourceName;

@ResourceName(parent = "security-group", value = "egress")
public class SecurityGroupEgressRuleResource extends SecurityGroupRuleResource {

    @Override
    public String resourceType() {
        return "egress";
    }

}
