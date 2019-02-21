package beam.aws.rds;

import beam.core.diff.Diffable;

import java.util.ArrayList;
import java.util.List;

public class OptionConfiguration extends Diffable {

    private String optionName;
    private List<OptionSetting> optionSetting;
    private Integer port;
    private String version;
    private List<String> vpcSecurityGroupMemberships;

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public List<OptionSetting> getOptionSetting() {
        if (optionSetting == null) {
            optionSetting = new ArrayList<>();
        }

        return optionSetting;
    }

    public void setOptionSetting(List<OptionSetting> optionSetting) {
        this.optionSetting = optionSetting;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

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
