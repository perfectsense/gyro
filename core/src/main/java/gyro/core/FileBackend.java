package gyro.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

public interface FileBackend {

    Stream<String> list();

    InputStream openInput(String file);

    OutputStream openOutput(String file);

}
