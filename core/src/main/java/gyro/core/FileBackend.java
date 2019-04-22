package gyro.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileBackend {

    String name();

    InputStream read(String file) throws Exception;

    OutputStream write(String file) throws IOException;

    void delete(String file);

}
