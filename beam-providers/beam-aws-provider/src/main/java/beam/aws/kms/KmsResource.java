package beam.aws.kms;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.CompactMap;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.AliasListEntry;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
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
 *         description: "sample kms key update"
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


    @ResourceDiffProperty(updatable = true)
    public Boolean getBypassPolicyLockoutSafetyCheck() {
        return bypassPolicyLockoutSafetyCheck;
    }

    public void setBypassPolicyLockoutSafetyCheck(Boolean bypassPolicyLockoutSafetyCheck) {
        this.bypassPolicyLockoutSafetyCheck = bypassPolicyLockoutSafetyCheck;
    }


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

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }


    @ResourceDiffProperty(updatable = true)
    public String getKeyManager() {
        return keyManager;
    }

    /**
     * The manager of the key, either AWS or customer. (Optional)
     */
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
    private String getPolicyContents() {
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

        DescribeKeyResponse keyResponse = client.describeKey(r -> r.keyId(getKeyId()));
        if (keyResponse != null) {

            KeyMetadata keyMetadata = keyResponse.keyMetadata();
            //only load keys that are NOT pending deletion
            if(!keyMetadata.keyStateAsString().equals("PENDING_DELETION")) {

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
        }

        return false;
    }

