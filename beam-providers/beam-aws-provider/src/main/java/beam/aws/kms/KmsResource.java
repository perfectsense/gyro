package beam.aws.kms;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.CompactMap;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.AliasListEntry;
import software.amazon.awssdk.services.kms.model.AlreadyExistsException;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KmsException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.awssdk.services.kms.model.Tag;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::kms kms-example
 *         aliases: ["alias/kmsExample", "alias/kmsSecondExample"]
 *         bypass-policy-lockout-safety-check: "false"
 *         description: "sample kms key"
 *         enabled: "true"
 *         key-manager: "CUSTOMER"
 *         key-rotation: "false"
 *         key-usage: "ENCRYPT_DECRYPT"
 *         origin: "AWS_KMS"
 *         pending-window: "7"
 *         policy: "beam-providers/beam-aws-provider/examples/kms/kms-policy.json"
 *         tags: {
 *               Name: "kms-example"
 *         }
 * end
 */

@ResourceName("kms")
public class KmsResource extends AwsResource {

    private List<String> aliases;
    private Boolean bypassPolicyLockoutSafetyCheck;
    private String customKeyStoreId;
    private String description;
    private Boolean enabled;
    private String keyArn;
    private String keyId;
    private String keyManager;
    private Boolean keyRotation;
    private String keyState;
    private String keyUsage;
    private String origin;
    private String pendingWindow;
    private String policy;
    private String policyContents;
    private Map<String, String> tags;

    /**
     * The list of aliases associated with the key.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getAliases() {
        if (aliases == null) {
            aliases = new ArrayList<>();
        }

        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * Determines whether to bypass the key policy lockout safety check.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getBypassPolicyLockoutSafetyCheck() {
        return bypassPolicyLockoutSafetyCheck;
    }

    public void setBypassPolicyLockoutSafetyCheck(Boolean bypassPolicyLockoutSafetyCheck) {
        this.bypassPolicyLockoutSafetyCheck = bypassPolicyLockoutSafetyCheck;
    }

    /**
     * Creates the key in the specified custom key store.
     */
    @ResourceDiffProperty(updatable = true)
    public String getCustomKeyStoreId() {
        return customKeyStoreId;
    }

    public void setCustomKeyStoreId(String customKeyStoreId) {
        this.customKeyStoreId = customKeyStoreId;
    }

    /**
     * The description of the key.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Determines whether the key is enabled.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Determines whether the backing key is rotated each year.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getKeyRotation() {
        return keyRotation;
    }

    public void setKeyRotation(Boolean keyRotation) {
        this.keyRotation = keyRotation;
    }

    public String getKeyArn() {
        return keyArn;
    }

    public void setKeyArn(String keyArn) {
        this.keyArn = keyArn;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /**
     * The manager of the key, either AWS or customer. (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getKeyManager() {
        return keyManager;
    }

    public void setKeyManager(String keyManager) {
        this.keyManager = keyManager;
    }

    public String getKeyState() {
        return keyState;
    }

    public void setKeyState(String keyState) {
        this.keyState = keyState;
    }

    /**
     * The usage of the key, which is encryption and decryption. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getKeyUsage() {
        return keyUsage;
    }

    public void setKeyUsage(String keyUsage) {
        this.keyUsage = keyUsage;
    }

    /**
     * The source of the key material. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * The number of days until the key will be deleted. (Required)
     */
    public String getPendingWindow() {
        return pendingWindow;
    }

    public void setPendingWindow(String pendingWindow) {
        this.pendingWindow = pendingWindow;
    }

    /**
     * The policy associated with the key. (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPolicyContents() {
        if (policyContents != null) {
            return this.policyContents;
        } else {
            if (getPolicy() != null) {
                try {
                    String encode = new String(Files.readAllBytes(Paths.get(getPolicy())), "UTF-8");
                    return formatPolicy(encode);
                } catch (Exception err) {
                    throw new BeamException(err.getMessage());
                }
            } else {
                return null;
            }
        }
    }

    public void setPolicyContents(String policyContents) {
        this.policyContents = policyContents;
    }

    /**
     * The tags associated with the key. (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    @Override
    public boolean refresh() {
        KmsClient client = createClient(KmsClient.class);

        try {
            DescribeKeyResponse keyResponse = client.describeKey(r -> r.keyId(getKeyId()));
            KeyMetadata keyMetadata = keyResponse.keyMetadata();
            if (!keyMetadata.keyStateAsString().equals("PENDING_DELETION")) {

                setCustomKeyStoreId(keyMetadata.customKeyStoreId());
                setDescription(keyMetadata.description());
                setEnabled(keyMetadata.enabled());
                setKeyManager(keyMetadata.keyManagerAsString());
                setKeyState(keyMetadata.keyStateAsString());
                setKeyUsage(keyMetadata.keyUsageAsString());
                setOrigin(keyMetadata.originAsString());

                getAliases().clear();
                ListAliasesResponse aliasResponse = client.listAliases(r -> r.keyId(getKeyId()));
                if (aliasResponse != null) {
                    for (AliasListEntry alias : aliasResponse.aliases()) {
                        getAliases().add(alias.aliasName());
                    }
                }

                GetKeyPolicyResponse policyResponse = client.getKeyPolicy(r -> r.keyId(getKeyId()).policyName("default"));
                if (policyResponse != null) {
                    setPolicyContents(formatPolicy(policyResponse.policy()));
                }
            }

            return true;

        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public void create() {
        KmsClient client = createClient(KmsClient.class);

        List<String> newList = getAliases().stream()
                .distinct()
                .collect(Collectors.toList());

        if (newList.size() == getAliases().size()) {

            CreateKeyResponse response = client.createKey(
                    r -> r.bypassPolicyLockoutSafetyCheck(getBypassPolicyLockoutSafetyCheck())
                            .customKeyStoreId(getCustomKeyStoreId())
                            .description(getDescription())
                            .keyUsage(getKeyUsage())
                            .origin(getOrigin())
                            .policy(getPolicyContents())
                            .tags(toTag())
            );

            setKeyId(response.keyMetadata().keyId());
            setKeyManager(response.keyMetadata().keyManagerAsString());
            setKeyState(response.keyMetadata().keyStateAsString());

            if (getAliases() != null) {
                for (String alias : getAliases()) {
                    client.createAlias(r -> r.aliasName(alias).targetKeyId(getKeyId()));
                }
            }

            if (getKeyRotation() != null && getKeyRotation() == true) {
                client.enableKeyRotation(r -> r.keyId(getKeyId()));
            }

            if (getEnabled() == false) {
                client.disableKey(r -> r.keyId(getKeyId()));
            }
        } else {
            throw new BeamException("Duplicate aliases are not allowed in the same region");
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        KmsClient client = createClient(KmsClient.class);
        KmsResource currentResource = (KmsResource) current;

        //update tags
        client.tagResource(r -> r.tags(toTag())
                                .keyId(getKeyId()));

        //update description
        client.updateKeyDescription(r -> r.description(getDescription())
                                            .keyId(getKeyId()));

        //key rotation
        if (getKeyRotation() == true && currentResource.getKeyRotation() == false) {
            client.enableKeyRotation(r -> r.keyId(getKeyId()));
        } else if (getKeyRotation() == false && currentResource.getKeyRotation() == true) {
            client.disableKeyRotation(r -> r.keyId(getKeyId()));
        }

        //enable/disable
        if (getEnabled() == true && currentResource.getEnabled() == false) {
            client.enableKey(r -> r.keyId(getKeyId()));
        } else if (getEnabled() == false && currentResource.getEnabled() == true) {
            client.disableKey(r -> r.keyId(getKeyId()));
        }

        //update key policy
        client.putKeyPolicy(r -> r.policy(getPolicyContents())
                                    .policyName("default")
                                    .keyId(getKeyId()));

        //update alias
        List<String> aliasAdditions = new ArrayList<>(getAliases());
        aliasAdditions.removeAll(currentResource.getAliases());

        List<String> aliasSubtractions = new ArrayList<>(currentResource.getAliases());
        aliasSubtractions.removeAll(getAliases());

        for (String add : aliasAdditions) {
            client.createAlias(r -> r.aliasName(add).targetKeyId(getKeyId()));
        }

        for (String sub : aliasSubtractions) {
            client.deleteAlias(
                    r -> r.aliasName(sub));
        }
    }

    @Override
    public void delete() {
        KmsClient client = createClient(KmsClient.class);
        client.scheduleKeyDeletion(r -> r.keyId(getKeyId())
                                        .pendingWindowInDays(7));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("kms key with alias " + getAliases().get(0));
        return sb.toString();
    }

    private List<Tag> toTag() {
        List<Tag> tag = new ArrayList<>();
        getTags().forEach((key, value) -> tag.add(Tag.builder().tagKey(key).tagValue(value).build()));
        return tag;
    }

    private String formatPolicy(String policy) {
        return policy != null ? policy.replaceAll(System.lineSeparator(), " ").replaceAll("\t", " ").trim().replaceAll(" ", "") : policy;
    }
}
