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
import software.amazon.awssdk.services.rds.model.Parameter;
import software.amazon.awssdk.services.rds.paginators.DescribeDBParametersIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create a db parameter group.
 *
 * .. code-block:: beam
 *
 *    aws::db-parameter-group parameter-group
 *        name: "parameter-group-example"
 *        description: "some description"
 *        family: "mysql5.6"
 *        parameter
 *            name: "autocommit"
 *            value: "1"
 *        end
 *
 *        parameter
 *            name: "character_set_client"
 *            value: "utf8"
 *        end
 *
 *        tags: {
 *            Name: "db-parameter-group-example"
 *        }
 *    end
 */
@ResourceName("db-parameter-group")
public class DbParameterGroupResource extends RdsTaggableResource {

    private String description;
    private String family;
    private String name;
    private List<DbParameter> parameter;

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
    public List<DbParameter> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<>();
        }

        return parameter;
    }

    public void setParameter(List<DbParameter> parameter) {
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

            DescribeDBParametersIterable iterable = client.describeDBParametersPaginator(
                r -> r.dbParameterGroupName(getName())
            );

            Set<String> names = getParameter().stream().map(DbParameter::getName).collect(Collectors.toSet());
            getParameter().clear();
            iterable.stream().forEach(
                r -> getParameter().addAll(r.parameters().stream()
                    .filter(p -> names.contains(p.parameterName()))
                    .map(p -> {
                                DbParameter parameter = new DbParameter();
                                parameter.setApplyMethod(p.applyMethodAsString());
                                parameter.setName(p.parameterName());
                                parameter.setValue(p.parameterValue());
                                return parameter;
                            }
                        )
                    .collect(Collectors.toList())
                )
            );

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
        return "db parameter group " + getName();
    }
}
