package gyro.plugin.enterprise;

import gyro.lang.plugins.Provider;

public class EnterprisePlugin extends Provider {

    @Override
    public String name() {
        return "enterprise";
    }

    @Override
    public void init() {
        getScope().getRootScope().getResourceClasses().put("aws::credentials", EnterpriseAwsCredentials.class);
    }

}
