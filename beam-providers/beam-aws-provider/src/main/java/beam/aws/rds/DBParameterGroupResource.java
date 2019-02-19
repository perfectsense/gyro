package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbParameterGroupResponse;
import software.amazon.awssdk.services.rds.model.DbParameterGroupNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbParameterGroupsResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbParametersResponse;
import software.amazon.awssdk.services.rds.model.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName("db-parameter-group")
public class DBParameterGroupResource extends RdsTaggableResource {

    private String description;
    private String family;
    private String name;
    private List<DBParameter> parameter;

    /**
     * The description of the DB parameter group. (Required)
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The name of the DB parameter group family. (Required)
     */
    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    /**
     * The name of the DB parameter group. (Required)
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A list of DB parameters.
     */
    @ResourceDiffProperty(updatable = true)
    public List<DBParameter> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<>();
        }

        return parameter;
    }

    public void setParameter(List<DBParameter> parameter) {
        this.parameter = parameter;
    }

    @Override
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getName())) {
            throw new BeamException("name is missing, unable to load db parameter group.");
        }

        try {
            DescribeDbParameterGroupsResponse response = client.describeDBParameterGroups(
                r -> r.dbParameterGroupName(getName())
            );

            response.dbParameterGroups().stream()
                .forEach(g -> {
                        setFamily(g.dbParameterGroupFamily());
                        setName(g.dbParameterGroupName());
                        setDescription(g.description());
                        setArn(g.dbParameterGroupArn());
                    }
                );


            DescribeDbParametersResponse parametersResponse = client.describeDBParameters(
                r -> r.dbParameterGroupName(getName())
            );

            Set<String> names = getParameter().stream().map(DBParameter::getName).collect(Collectors.toSet());
            getParameter().clear();
            getParameter().addAll(parametersResponse.parameters().stream()
                .filter(p -> names.contains(p.parameterName()))
                .map(p -> {
                            DBParameter parameter = new DBParameter();
                            parameter.setApplyMethod(p.applyMethodAsString());
                            parameter.setName(p.parameterName());
                            parameter.setValue(p.parameterValue());
                            return parameter;
                        }
                    )
                .collect(Collectors.toList()));

        } catch (DbParameterGroupNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        CreateDbParameterGroupResponse response = client.createDBParameterGroup(
            r -> r.dbParameterGroupFamily(getFamily())
                    .dbParameterGroupName(getName())
                    .description(getDescription())
        );

        setArn(response.dbParameterGroup().dbParameterGroupArn());
        modifyParameterGroup();
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {
        modifyParameterGroup();
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteDBParameterGroup(r -> r.dbParameterGroupName(getName()));
    }

    private void modifyParameterGroup() {
        RdsClient client = createClient(RdsClient.class);
        client.modifyDBParameterGroup(
            r -> r.dbParameterGroupName(getName())
                .parameters(getParameter().stream().map(
                    p -> Parameter.builder()
                        .parameterName(p.getName())
                        .parameterValue(p.getValue())
                        .applyMethod(p.getApplyMethod())
                        .build())
                    .collect(Collectors.toList()))
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("db parameter group ");
        sb.append(getName());

        return sb.toString();
    }
}
