package gyro.core.vault;

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.Namespace;
import gyro.core.Type;
import gyro.core.resource.Resource;
import gyro.core.scope.State;
import org.apache.commons.codec.binary.Base64;

@Type("vault-secret")
@Namespace("gyro")
public class LocalVaultSecretResource extends Resource {

    private String iv;
    private String salt;
    private String cipher;
    private String encryptedData;

    public LocalVaultSecretResource() {
    }

    public LocalVaultSecretResource(byte[] iv, byte[] salt, String cipher, byte[] encryptedData) {
        this.iv = Base64.encodeBase64String(iv);
        this.salt = Base64.encodeBase64String(salt);
        this.cipher = cipher;
        this.encryptedData = Base64.encodeBase64String(encryptedData);
    }

    public String getIv() {
        return iv;
    }

    public byte[] getIvAsBytes() {
        return Base64.decodeBase64(getIv());
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getSalt() {
        return salt;
    }

    public byte[] getSaltAsBytes() {
        return Base64.decodeBase64(getSalt());
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public byte[] getEncryptedDataAsBytes() {
        return Base64.decodeBase64(getEncryptedData());
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create(GyroUI ui, State state) throws Exception {

    }

    @Override
    public void update(GyroUI ui, State state, Resource current, Set<String> changedFieldNames) throws Exception {

    }

    @Override
    public void delete(GyroUI ui, State state) throws Exception {

    }

}
