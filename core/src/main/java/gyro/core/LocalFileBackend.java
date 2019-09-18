package gyro.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class LocalFileBackend extends FileBackend {

    private final Path rootDirectory;

    public LocalFileBackend(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Stream<String> list() throws IOException {
        if (Files.exists(rootDirectory)) {
            return Files.find(rootDirectory, Integer.MAX_VALUE, (file, attributes) -> attributes.isRegularFile())
                .map(rootDirectory::relativize)
                .map(Path::toString)
                .filter(f -> !f.startsWith(".gyro/") && f.endsWith(".gyro"));

        } else {
            return Stream.empty();
        }
    }

    @Override
    public InputStream openInput(String file) throws IOException {
        return Files.newInputStream(rootDirectory.resolve(file).normalize());
    }

    @Override
    public OutputStream openOutput(String file) throws IOException {
        Path finalFile = rootDirectory.resolve(file);
        Path finalDir = finalFile.getParent();

        Files.createDirectories(finalDir);

        Path tempFile = Files.createTempFile(finalDir, ".local-file-backend-", ".gyro.tmp");

        tempFile.toFile().deleteOnExit();

        return new FileOutputStream(tempFile.toString()) {

            @Override
            public void close() throws IOException {
                super.close();
                Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }

    @Override
    public void delete(String file) throws IOException {
        Files.deleteIfExists(rootDirectory.resolve(file));
    }

    @Override
    public String toString() {
        return rootDirectory.toString();
    }

}
