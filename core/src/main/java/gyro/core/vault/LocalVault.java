package gyro.core.vault;

import com.google.common.collect.ImmutableSet;
import com.psddev.dari.util.IoUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.core.Type;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodePrinter;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.PrinterContext;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.util.Bug;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

@Type("local")
public class LocalVault extends Vault {

    private String keyPath;
    private String cipher;
    private Integer keyLength;
    private Integer keyIterations;

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public Integer getKeyLength() {
        if (keyLength == null) {
            return 256;
        }

        return keyLength;
    }

    public void setKeyLength(Integer keyLength) {
        this.keyLength = keyLength;
    }

    public Integer getKeyIterations() {
        if (keyIterations == null) {
            return 65536;
        }

        return keyIterations;
    }

    public void setKeyIterations(Integer keyIterations) {
        if (keyIterations != null && keyIterations < 10000) {
            throw new GyroException(String.format(Locale.getDefault(), "Key iterations must be greater than %,d per NIST recommended guidelines.", 10000));
        }

        this.keyIterations = keyIterations;
    }

    public Path vaultPath() {
        return GyroCore.getRootDirectory().resolve(Paths.get(".gyro", "vault", getName()));
    }

    public char[] key() {
        try {
            byte[] key = Base64.decodeBase64(IoUtils.toByteArray(new File(getKeyPath())));
            return new String(key, StandardCharsets.UTF_8).toCharArray();
        } catch (FileNotFoundException ex) {
            throw new GyroException("Encryption key for '" + getName() + "' vault not found at path '" + getKeyPath() + "'");
        } catch (IOException ex) {
            throw new GyroException("Unable to load encryption key for '" + getName() + "' vault.", ex);
        }
    }

    @Override
    public String get(String key) {
        RootScope vaultScope = loadVault();
        DiffableType type = DiffableType.getInstance(LocalVaultSecretResource.class);
        LocalVaultSecretResource secret = (LocalVaultSecretResource) vaultScope.findResource(type.getName() + "::" + key);

        if (secret != null) {
            return decrypt(secret);
        }

        return null;
    }

    @Override
    public boolean put(String key, String value) {
        RootScope vaultScope = loadVault();
        DiffableType type = DiffableType.getInstance(LocalVaultSecretResource.class);
        LocalVaultSecretResource secret = encrypt(value);
        DiffableInternals.setName(secret, key);

        String name = type.getName() + "::" + key;
        boolean overwritten = vaultScope.containsKey(name);

        vaultScope.put(name, secret);
        save(vaultScope);

        return overwritten;
    }

    @Override
    public Map<String, String> list(String prefix) {
        Map<String, String> secrets = new HashMap<>();

        RootScope vaultScope = loadVault();
        vaultScope.findResourcesByClass(LocalVaultSecretResource.class)
            .filter(s -> prefix == null || DiffableInternals.getName(s).startsWith(prefix))
            .forEach(s -> secrets.put(DiffableInternals.getName(s), decrypt(s)));

        return secrets;
    }

    private RootScope loadVault() {
        if (!Files.exists(vaultPath())) {
            try {
                Files.createDirectories(vaultPath().getParent());
                Files.createFile(vaultPath(), PosixFilePermissions.asFileAttribute(ImmutableSet.of(OWNER_READ, OWNER_WRITE)));
            } catch (Exception ex) {
                throw new GyroException(ex);
            }
        }

        RootScope vault = new RootScope(
            vaultPath().toString(),
            new LocalFileBackend(GyroCore.getRootDirectory()),
            null,
            ImmutableSet.of());

        vault.getRootScope().put(DiffableType.getInstance(LocalVaultSecretResource.class).getName(), LocalVaultSecretResource.class);

        try {
            vault.evaluate();
        } catch (GyroException ex) {
            if (!(ex.getCause() instanceof NoSuchFileException)) {
                throw ex;
            }
        }

        return vault;
    }

    private void save(RootScope vaultScope) {
        NodePrinter printer = new NodePrinter();

        try (PrintWriter out = new PrintWriter(
            new OutputStreamWriter(
                vaultScope.openOutput(vaultPath().toString()),
                StandardCharsets.UTF_8))) {

            PrinterContext context = new PrinterContext(out, 0);

            for (Object value : vaultScope.values()) {
                if (value instanceof Resource) {
                    Resource resource = (Resource) value;

                    printer.visit(
                        new ResourceNode(
                            DiffableType.getInstance(resource.getClass()).getName(),
                            new ValueNode(DiffableInternals.getName(resource)),
                            toBodyNodes(resource)),
                        context);
                }
            }

        } catch (IOException error) {
            throw new Bug(error);
        }
    }

    private List<Node> toBodyNodes(Diffable diffable) {
        List<Node> body = new ArrayList<>();

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            Object value = field.getValue(diffable);

            if (value == null) {
                continue;
            }

            String key = field.getName();

            if (value instanceof Boolean
                || value instanceof Map
                || value instanceof Number
                || value instanceof String) {

                body.add(toPairNode(key, value));

            } else if (value instanceof Date) {
                body.add(toPairNode(key, value.toString()));

            } else if (value instanceof Enum<?>) {
                body.add(toPairNode(key, ((Enum) value).name()));

            } else if (value instanceof Diffable) {
                if (field.shouldBeDiffed()) {
                    body.add(new KeyBlockNode(key, null, toBodyNodes((Diffable) value)));

                } else {
                    body.add(toPairNode(key, value));
                }

            } else if (value instanceof Collection) {
                if (field.shouldBeDiffed()) {
                    for (Object item : (Collection<?>) value) {
                        body.add(new KeyBlockNode(key, null, toBodyNodes((Diffable) item)));
                    }

                } else {
                    body.add(toPairNode(key, value));
                }

            } else {
                throw new GyroException(String.format(
                    "Can't convert @|bold %s|@, an instance of @|bold %s|@, into a node!",
                    value,
                    value.getClass().getName()));
            }
        }

        return body;
    }

    private PairNode toPairNode(Object key, Object value) {
        return new PairNode(toNode(key), toNode(value));
    }

    private Node toNode(Object value) {
        if (value instanceof Boolean
            || value instanceof Number
            || value instanceof String) {

            return new ValueNode(value);

        } else if (value instanceof Collection) {
            List<Node> items = new ArrayList<>();

            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    items.add(toNode(item));
                }
            }

            return new ListNode(items);

        } else if (value instanceof Map) {
            List<PairNode> entries = new ArrayList<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object v = entry.getValue();

                if (v != null) {
                    entries.add(toPairNode(entry.getKey(), v));
                }
            }

            return new MapNode(entries);

        } else if (value instanceof Resource) {
            Resource resource = (Resource) value;
            DiffableType type = DiffableType.getInstance(resource.getClass());

            if (DiffableInternals.isExternal(resource)) {
                return new ValueNode(type.getIdField().getValue(resource));

            } else {
                return new ReferenceNode(
                    Arrays.asList(
                        new ValueNode(type.getName()),
                        new ValueNode(DiffableInternals.getName(resource))),
                    Collections.emptyList());
            }

        } else {
            throw new GyroException(String.format(
                "Can't convert @|bold %s|@, an instance of @|bold %s|@, into a node!",
                value,
                value.getClass().getName()));
        }
    }

    private String decrypt(LocalVaultSecretResource secret) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(key(), secret.getSaltAsBytes(), getKeyIterations(), getKeyLength());

            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec skey = new SecretKeySpec(tmp.getEncoded(), "AES");

            IvParameterSpec ivspec = new IvParameterSpec(secret.getIvAsBytes());

            Cipher cipher = Cipher.getInstance(getCipher());
            cipher.init(Cipher.DECRYPT_MODE, skey, ivspec);

            byte[] decryptedBytes = cipher.doFinal(secret.getEncryptedDataAsBytes());

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new GyroException("Error decrypting vault data: ", ex);
        }
    }

    private LocalVaultSecretResource encrypt(String plaintext) {
        try {
            byte[] salt = new byte[8];
            SecureRandom.getInstanceStrong().nextBytes(salt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(key(), salt, getKeyIterations(), getKeyLength());

            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec skey = new SecretKeySpec(tmp.getEncoded(), "AES");

            byte[] iv = new byte[128/8];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(getCipher());
            cipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);

            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new LocalVaultSecretResource(iv, salt, getCipher(), encryptedBytes);
        } catch (Exception ex) {
            throw new GyroException("Error encrypting vault data: ", ex);
        }
    }

}
