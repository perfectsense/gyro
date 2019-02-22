package beam.aws.rds;

import beam.core.diff.Diffable;

import java.util.ArrayList;
import java.util.List;

public class OptionConfiguration extends Diffable {

    private String optionName;
    private List<OptionSettings> optionSettings;
    private Integer port;
    private String version;
    private List<String> vpcSecurityGroupMemberships;

    /**
     * The option name.
     */
    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    /**
     * The option settings to include in an option group.
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
     * The optional port for the option.
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * The version for the option.
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * A list of VpcSecurityGroupMemebrship name strings used for this option.
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
