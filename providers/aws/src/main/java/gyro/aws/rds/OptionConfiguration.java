package gyro.aws.rds;

import gyro.core.diff.Diffable;

import java.util.ArrayList;
import java.util.List;

public class OptionConfiguration extends Diffable {

    private String optionName;
    private List<OptionSettings> optionSettings;
    private Integer port;
    private String version;
    private List<String> vpcSecurityGroupMemberships;

    /**
     * The name of the option.
     */
    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    /**
     * The List of option settings to include in the option configuration.
     *
     * @subresource gyro.aws.rds.OptionSettings
     */
    public List<OptionSettings> getOptionSettings() {
        if (optionSettings == null) {
            optionSettings = new ArrayList<>();
        }

        return optionSettings;
    }

    public void setOptionSettings(List<OptionSettings> optionSettings) {
        this.optionSettings = optionSettings;
    }

    /**
     * The port of the option.
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * The version of the option.
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * A list of VPC security groups used for this option.
     */
    public List<String> getVpcSecurityGroupMemberships() {
        if (vpcSecurityGroupMemberships == null) {
            vpcSecurityGroupMemberships = new ArrayList<>();
        }

        return vpcSecurityGroupMemberships;
    }

    public void setVpcSecurityGroupMemberships(List<String> vpcSecurityGroupMemberships) {
        this.vpcSecurityGroupMemberships = vpcSecurityGroupMemberships;
    }

    @Override
    public String primaryKey() {
        return getOptionName();
    }

    @Override
    public String toDisplayString() {
        return "option configuration " + getOptionName();
    }
}
