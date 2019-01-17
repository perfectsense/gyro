package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

/**
 * Creates a key pair using the public key provided.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::key-pair key-pair-example
 *         key-name: "key-pair-example"
 *         public-key-path: "beam-providers/beam-aws-provider/examples/ec2/example-public-key.pub"
 *     end
 */
@ResourceName("key-pair")
public class KeyPairResource extends AwsResource {

    private String keyName;
    private String publicKeyPath;
    private String keyFingerPrint;

    /**
     * The key name that you want to assign for your key pair. See `Amazon EC2 Key Pairs <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html/>`_. (Required)
     */
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    /**
     * The file path that contains the public key needed to generate the key pair. See `Importing Your Own Public Key to Amazon EC2 <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#how-to-generate-your-own-key-and-import-it-to-aws/>`_. (Required)
     */
    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public String getKeyFingerPrint() {
        return keyFingerPrint;
    }

    public void setKeyFingerPrint(String keyFingerPrint) {
        this.keyFingerPrint = keyFingerPrint;
    }

    @Override
    public boolean refresh() {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getKeyName())) {
            throw new BeamException("key-name is missing, unable to load key pair.");
        }

        try {
            DescribeKeyPairsResponse response = client.describeKeyPairs(r -> r.keyNames(Collections.singleton(getKeyName())));
            setKeyFingerPrint(response.keyPairs().get(0).keyFingerprint());
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return false;
            }

            throw ex;
        }

        return true;
    }

    @Override
    public void create() {
        Ec2Client client = createClient(Ec2Client.class);

        ImportKeyPairResponse response = client.importKeyPair(
            r -> r.keyName(getKeyName())
                .publicKeyMaterial(SdkBytes.fromByteArray(getPublicKeyFromPath().getBytes()))
        );

        setKeyFingerPrint(response.keyFingerprint());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteKeyPair(r -> r.keyName(getKeyName()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Key Pair");

        if (!ObjectUtils.isBlank(getKeyName())) {
            sb.append(" - ").append(getKeyName());
        }

        return sb.toString();
    }

    private String getPublicKeyFromPath() {
        try {
            return (new String(Files.readAllBytes(Paths.get(getPublicKeyPath())), StandardCharsets.UTF_8));
        } catch (IOException ioex) {
            throw new BeamException("Unable to read public key from file.");
        }
    }
}
