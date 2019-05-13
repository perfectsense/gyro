package gyro.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class LocalFileBackend implements FileBackend {

    private final Path rootDirectory;

    public LocalFileBackend(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Stream<String> list() {
        if (Files.exists(rootDirectory)) {
            try {
                return Files.find(rootDirectory, Integer.MAX_VALUE, (file, attributes) -> attributes.isRegularFile())
                    .map(rootDirectory::relativize)
                    .map(Path::toString)
                    .filter(f -> !f.startsWith(".gyro/") && f.endsWith(".gyro"));

            } catch (IOException error) {
                throw new GyroException(
                    String.format("Can't list files in [%s]!", rootDirectory),
                    error);
            }

        } else {
            return Stream.empty();
        }
    }

    @Override
    public InputStream openInput(String file) {
        try {
            return Files.newInputStream(rootDirectory.resolve(file).normalize());

        } catch (IOException error) {
            throw new GyroException(
                String.format("Can't open [%s]!", file),
                error);
        }
    }

    @Override
    public OutputStream openOutput(String file) {
        try {
            Path tempFile = Files.createTempFile("local-file-backend-", ".gyro");

            tempFile.toFile().deleteOnExit();

            return new FileOutputStream(tempFile.toString()) {

                @Override
                public void close() throws IOException {
                    super.close();

                    Path f = rootDirectory.resolve(file);

                    Files.createDirectories(f.getParent());
                    Files.move(tempFile, f, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                }
            };

        } catch (IOException error) {
            throw new GyroException(
                String.format("Can't open [%s]!", file),
                error);
        }
    }

}
