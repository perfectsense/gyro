package gyro.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileBackend {

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws IOException;

}
