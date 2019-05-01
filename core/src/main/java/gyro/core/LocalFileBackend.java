package gyro.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalFileBackend implements FileBackend {

    private final Path rootDirectory;

    public LocalFileBackend(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        return Files.newInputStream(rootDirectory.resolve(file));
    }

    @Override
    public OutputStream openOutput(String file) throws IOException {
        Path tempFile = Files.createTempFile("local-file-backend-", ".gyro.state");

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
    }

}
