package beam.aws.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.KeyPairInfo;

public class KeyPairResource extends AWSResource<KeyPairInfo> {

    private String keyFingerprint;
    private String keyName;

    public String getKeyFingerprint() {
        return keyFingerprint;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getKeyName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, KeyPairInfo keyPair) {
        this.keyFingerprint = keyPair.getKeyFingerprint();
        setKeyName(keyPair.getKeyName());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateKeyPairRequest ckpRequest = new CreateKeyPairRequest();

        ckpRequest.setKeyName(getKeyName());

        CreateKeyPairResult keyPairResult;
        try {
            keyPairResult = client.createKeyPair(ckpRequest);
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("InvalidKeyPair.Duplicate")) {
                throw ase;
            }

            return;
        }

        // Store keypair in $HOME/.ssh/
        try {
            File pemFile = new File(System.getProperty("user.home") + "/.ssh/" + getKeyName() + "-" + cloud.getAccount() + ".pem");

            if (pemFile.exists()) {
                new BeamException(String.format("Key pair already exists at %s, not overwriting.", pemFile.toPath()));
            }

            FileWriter output = new FileWriter(pemFile);
            output.write(keyPairResult.getKeyPair().getKeyMaterial());

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(Paths.get(pemFile.getAbsolutePath()), perms);

            output.close();
        } catch (IOException ex) {

        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, KeyPairInfo> current, Set<String> changedProperties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DeleteKeyPairRequest dkpRequest = new DeleteKeyPairRequest();

        dkpRequest.setKeyName(getKeyName());
        client.deleteKeyPair(dkpRequest);
    }

    @Override
    public String toDisplayString() {
        return "key pair " + getKeyName();
    }
}
