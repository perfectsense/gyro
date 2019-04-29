package gyro.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalFileBackend implements FileBackend {

    @Override
    public InputStream openInput(String file) throws Exception {
        return new FileInputStream(file);
    }

    @Override
    public OutputStream openOutput(String file) throws IOException {
        Path newFile = Files.createTempFile("local-file-backend-", ".gyro.state");
        newFile.toFile().deleteOnExit();

        return new FileOutputStream(newFile.toString()) {

            @Override
            public void close() throws IOException {
                super.close();
                Files.move(
                    newFile,
                    Paths.get(file),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }

}
