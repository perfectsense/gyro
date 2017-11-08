package beam.openstack.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyPairResource extends OpenStackResource<KeyPair> {

    private String keyFingerprint;
    private String keyName;
    private String privateKey;
    private String publicKey;

    public String getKeyFingerprint() {
        return keyFingerprint;
    }

    public void setKeyFingerprint(String keyFingerprint) {
        this.keyFingerprint = keyFingerprint;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getKeyName().toLowerCase());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, KeyPair keypair) {
        setKeyName(keypair.getName());
        setKeyFingerprint(keypair.getFingerprint());
        setPublicKey(keypair.getPublicKey());
        setPrivateKey(keypair.getPrivateKey());
    }

    @Override
    public void create(OpenStackCloud cloud) {
        NovaApi api = cloud.createApi();
        KeyPairApi keyPairApi = api.getKeyPairApi(getRegion()).get();

        if (ObjectUtils.isBlank(getPublicKey())) {
            KeyPair keyPair = keyPairApi.create(getKeyName());
            init(cloud, null, keyPair);

            // Store keypair in $HOME/.ssh/
            try {
                File pemFile = new File(System.getProperty("user.home") + "/.ssh/" + getKeyName() + ".pem");

                if (pemFile.exists()) {
                    new BeamException(String.format("Key pair already exists at %s, not overwriting.", pemFile.toPath()));
                }

                FileWriter output = new FileWriter(pemFile);
                output.write(getPrivateKey());

                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(Paths.get(pemFile.getAbsolutePath()), perms);

                output.close();
            } catch (IOException ex) {

            }
        } else {
            KeyPair keyPair = keyPairApi.createWithPublicKey(getKeyName(), getPublicKey());
            init(cloud, null, keyPair);
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, KeyPair> current, Set<String> changedProperties) {
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        NovaApi api = cloud.createApi();
        KeyPairApi keyPairApi = api.getKeyPairApi(getRegion()).get();

        keyPairApi.delete(getKeyName());
    }

    @Override
    public String toDisplayString() {
        return "key pair " + getKeyName();
    }
}
