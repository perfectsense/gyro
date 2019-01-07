package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a Role Resource with the specified options.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::role-resource example-role
 *         description: testing the role functionality
 *         assumeRolePolicyDocumentFile:
 *         roleName: rta-test-role
 *
 *     end
 */

@ResourceName("role-resource")
public class IamRoleResource extends AwsResource {

    private String roleName;
    private String description;
    private Map<String, Object> assumeRolePolicyContents;
    private String assumeRolePolicyDocument;
    private String assumeRolePolicyDocumentFilename;
    private Set<IamPolicyResource> policies;

    @ResourceDiffProperty(updatable = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAssumeRolePolicyContents() {
        if (assumeRolePolicyContents != null) {
            return assumeRolePolicyContents;
        }

        try {
            Map<String, Object> policyMap = (Map<String, Object>) ObjectUtils.fromJson(getAssumeRolePolicyDocument());
            if (policyMap.containsKey("Version")) {
                policyMap.remove("Version");
            }

            try {
                List<Map<String, String>> attributeList = (List<Map<String, String>>) policyMap.get("Statement");
                String name = attributeList.get(0).get("Sid");
                if (name.equals("")) {
                    attributeList.get(0).put("Sid", getAssumeRolePolicyDocumentFilename());
                }
                sortMapList(policyMap);

            } catch (Exception error) {
                throw new BeamException("Invalid policy file format. " + error + getAssumeRolePolicyDocumentFilename());
            }

            return policyMap;

        } catch (Exception error) {
            throw Throwables.propagate(error);
        }
    }

    private void setAssumeRolePolicyContents(Map<String, Object> assumeRolePolicyContents) {
        this.assumeRolePolicyContents = assumeRolePolicyContents;
    }

    public String getAssumeRolePolicyDocument() {
        return this.assumeRolePolicyDocument;
    }

    public void setAssumeRolePolicyDocument(String assumeRolePolicyDocument) {
        this.assumeRolePolicyDocument = assumeRolePolicyDocument;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAssumeRolePolicyDocumentFilename() {
        return this.assumeRolePolicyDocumentFilename;
    }

    public void setAssumeRolePolicyDocumentFilename(String assumeRolePolicyDocumentFilename) {
        this.assumeRolePolicyDocumentFilename = assumeRolePolicyDocumentFilename;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<IamPolicyResource> getPolicies() {
        if (this.policies == null) {
            this.policies = new HashSet();
        }

        return this.policies;
    }

    public void setPolicies(Set<IamPolicyResource> policies) {
        this.policies = policies;
    }

    @ResourceDiffProperty(updatable = true)
    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }


    @Override
    public boolean refresh() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        Role response = client.getRole(r -> r.roleName(getRoleName())).role();
        if (response != null) {
            setRoleName(response.roleName());
            setDescription(response.description());
            setAssumeRolePolicyContents((Map) ObjectUtils.fromJson(StringUtils.decodeUri(response.assumeRolePolicyDocument())));
            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        try {
            preparePolicy();
        } catch (Exception err) {
            err.getMessage();
        }

        System.out.println("\nwhat is the role name "+getRoleName());
        System.out.println("\nwhat is the description name "+getDescription());


        CreateRoleRequest request = CreateRoleRequest.builder()
                .assumeRolePolicyDocument(ObjectUtils.toJson(getAssumeRolePolicyDocument()))
                .description(getDescription())
                .roleName(getRoleName())
                .build();

        client.createRole(request);
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        UpdateAssumeRolePolicyRequest updatePolicyRequest = UpdateAssumeRolePolicyRequest.builder()
                .policyDocument(ObjectUtils.toJson(this.getAssumeRolePolicyDocument()))
                .roleName(getRoleName())
                .build();

        client.updateAssumeRolePolicy(updatePolicyRequest);

        UpdateRoleRequest updateRoleRequest = UpdateRoleRequest.builder()
                .description(getDescription())
                .roleName(getRoleName())
                .build();

        client.updateRole(updateRoleRequest);

    }

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        DeleteRoleRequest request = DeleteRoleRequest.builder()
                .roleName(getRoleName())
                .build();

        client.deleteRole(request);
    }

    @SuppressWarnings("unchecked")
    private void sortMapList(Map<String, Object> map) {
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {
                sortMapList((Map<String, Object>) value);
            } else if (value instanceof List) {
                for (Object element : (List) value) {
                    if (element instanceof Map) {
                        sortMapList((Map<String, Object>) element);
                    } else {
                        Collections.sort((List) value);
                        break;
                    }
                }
            }
        }
    }

    public void preparePolicy() throws Exception {
        if (getAssumeRolePolicyDocumentFilename() == null) {
            setAssumeRolePolicyDocument("");
            setAssumeRolePolicyContents(new HashMap<>());
        } else {
            String policyName = getAssumeRolePolicyDocumentFilename();
            File policyFile = new File(policyName + ".json");
            if (!policyFile.exists()) {
                throw new BeamException("Policy file: " + policyName + ".json does not exist.");
            }

            setAssumeRolePolicyDocument(IoUtils.toString(policyFile, Charsets.UTF_8));
            if (!ObjectUtils.isBlank(getAssumeRolePolicyDocument())) {
                Map<String, Object> policyDetails = getAssumeRolePolicyContents();

                setAssumeRolePolicyDocument(ObjectUtils.toJson(policyDetails));
                setAssumeRolePolicyContents(policyDetails);

            } else {
                setAssumeRolePolicyDocument("");
                Map<String, Object> policyDetails = new HashMap<>();
                policyDetails.put("policy", "");
                setAssumeRolePolicyContents(policyDetails);
            }
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String roleName = getRoleName();

        if (roleName != null) {
            sb.append("role name " + roleName);

        } else {
            sb.append("role name ");
        }

        return sb.toString();
    }

}