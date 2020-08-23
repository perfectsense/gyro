package gyro.core.vault;

import java.util.Arrays;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.psddev.dari.util.StringUtils;
import gyro.core.resource.CustomValue;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;

public class VaultSecret implements CustomValue {

    private String key;
    private String vault;
    private String value;
    private String hash;

    public VaultSecret(String key, String vault, String value, String hash) {
        this.key = key;
        this.vault = vault;
        this.value = value;
        this.hash = hash;
    }

    public String getKey() {
        return key;
    }

    public String getVault() {
        return vault;
    }

    public String getValue() {
        return value;
    }

    public String getHash() {
        if (hash != null) {
            return hash;
        }

        return StringUtils.hex(StringUtils.sha512(getValue()));
    }

    @Override
    public String toString() {
        return "<vault secret>";
    }

    @Override
    public Node toStateNode() {
        ValueNode vaultResolver = new ValueNode("vault-lookup");
        ValueNode vaultKey = new ValueNode(getKey());
        ValueNode vaultNameNode = new ValueNode(getVault());
        ValueNode vaultHash = new ValueNode(getHash());

        return new ReferenceNode(Arrays.asList(vaultResolver, vaultKey, vaultNameNode, vaultHash), ImmutableList.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof String) {
            return Objects.equals(getValue(), o);
        }

        if (!(o instanceof VaultSecret)) {
            return false;
        }

        VaultSecret that = (VaultSecret) o;

        return Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

}
