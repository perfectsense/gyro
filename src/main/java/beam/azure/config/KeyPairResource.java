package beam.azure.config;

import beam.BeamException;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.BeamRuntime;
import beam.azure.AzureCloud;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;


public class KeyPairResource extends AzureResource<Void> {

    private String name;
    private String publicKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, Void cloudResource) {
        String keyName = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), getRegion());
        setName(keyName);
    }

    @Override
    public void create(AzureCloud cloud) {
        String pemPath = System.getProperty("user.home") + "/.ssh/" + getName() + ".pem";
        String pubPath = System.getProperty("user.home") + "/.ssh/" + getName() + ".pub";

        try {
            File pemFile = new File(pemPath);
            JSch jsch = new JSch();
            KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            kpair.writePrivateKey(pemPath);
            kpair.writePublicKey(pubPath, "");
            kpair.dispose();

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(Paths.get(pemFile.getAbsolutePath()), perms);

            String publicKey = new Scanner(new File(pubPath)).useDelimiter("\\Z").next();
            setPublicKey(publicKey);

        } catch (Exception ex) {
            throw new BeamException("Fail to create key pair: " + getName() + "\ncause by: " + ex.getMessage());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, Void> current, Set<String> changedProperties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AzureCloud cloud) {

    }

    @Override
    public String toDisplayString() {
        return "key pair " + getName();
    }
}