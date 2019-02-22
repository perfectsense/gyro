package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateOptionGroupResponse;
import software.amazon.awssdk.services.rds.model.DescribeOptionGroupsResponse;
import software.amazon.awssdk.services.rds.model.OptionGroupNotFoundException;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName("db-option-group")
public class DbOptionGroupResource extends RdsTaggableResource {

    private String name;
    private String description;
    private String engine;
    private String majorEngineVersion;
    private List<OptionConfiguration> option;

    /**
     * Specifies the name of the option group to be created.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The description of the option group.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Specifies the name of the engine that this option group should be associated with.
     */
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Specifies the major version of the engine that this option group should be associated with.
     */
    public String getMajorEngineVersion() {
        return majorEngineVersion;
    }

    public void setMajorEngineVersion(String majorEngineVersion) {
        this.majorEngineVersion = majorEngineVersion;
    }

    /**
     * A list of Options to apply.
     */
    @ResourceDiffProperty(updatable = true)
    public List<OptionConfiguration> getOption() {
        if (option == null) {
            option = new ArrayList<>();
        }

        return option;
    }

    public void setOption(List<OptionConfiguration> option) {
        this.option = option;
    }

    @Override
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getName())) {
            throw new BeamException("name is missing, unable to load db option group.");
        }

        try {
            DescribeOptionGroupsResponse response = client.describeOptionGroups(
                r -> r.optionGroupName(getName())
            );

            response.optionGroupsList().stream()
                .forEach(g -> {
                        setDescription(g.optionGroupDescription());
                        setEngine(g.engineName());
                        setMajorEngineVersion(g.majorEngineVersion());
                        setOption(g.options().stream()
                            .map(o -> {
                                OptionConfiguration optionConfiguration = new OptionConfiguration();
                                optionConfiguration.setOptionName(o.optionName());
                                optionConfiguration.setPort(o.port());
                                optionConfiguration.setVersion(o.optionVersion());

                                optionConfiguration.setVpcSecurityGroupMemberships(
                                    o.vpcSecurityGroupMemberships().stream()
                                        .map(VpcSecurityGroupMembership::vpcSecurityGroupId)
                                        .collect(Collectors.toList()));

                                optionConfiguration.setOptionSetting(o.optionSettings().stream()
                                    .map(s -> {
                                        OptionSetting optionSetting = new OptionSetting();
                                        optionSetting.setName(s.name());
                                        optionSetting.setValue(s.value());
                                        return optionSetting;
                                    })
                                    .collect(Collectors.toList()));

                                return optionConfiguration;
                            })
                            .collect(Collectors.toList()));

                        setArn(g.optionGroupArn());
                    }
                );

        } catch (OptionGroupNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        CreateOptionGroupResponse response = client.createOptionGroup(
            r -> r.engineName(getEngine())
                    .optionGroupDescription(getDescription())
                    .majorEngineVersion(getMajorEngineVersion())
                    .optionGroupName(getName())
        );

        setArn(response.optionGroup().optionGroupArn());
        modifyOptionGroup(new ArrayList<>());
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {

        DbOptionGroupResource current = (DbOptionGroupResource) config;
        List<OptionConfiguration> removeList = current.getOption().stream()
            .filter(o -> !getOption().stream()
                .map(OptionConfiguration::getOptionName)
                .collect(Collectors.toList())
                .contains(o.getOptionName()))
            .collect(Collectors.toList());

        modifyOptionGroup(removeList);
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteOptionGroup(
            r -> r.optionGroupName(getName())
        );
    }

    private void modifyOptionGroup(List<OptionConfiguration> removeList) {
        RdsClient client = createClient(RdsClient.class);
        client.modifyOptionGroup(
            r -> r.optionGroupName(getName())
                .optionsToInclude(getOption().stream()
                    .map(o -> software.amazon.awssdk.services.rds.model.OptionConfiguration.builder()
                        .optionName(o.getOptionName())
                        .optionVersion(o.getVersion())
                        .port(o.getPort())
                        .vpcSecurityGroupMemberships(o.getVpcSecurityGroupMemberships())
                        .optionSettings(o.getOptionSetting().stream()
                            .map(s -> software.amazon.awssdk.services.rds.model.OptionSetting.builder()
                                .name(s.getName())
                                .value(s.getValue())
                                .build())
                            .collect(Collectors.toList()))
                        .build())
                    .collect(Collectors.toList()))

                .optionsToRemove(removeList.stream()
                    .map(OptionConfiguration::getOptionName)
                    .collect(Collectors.toList()))
        );
    }

    @Override
    public String toDisplayString() {
        return "db option group " + getName();
    }
}
