package gyro.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

public interface FileBackend {

    Stream<String> list() throws Exception;

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws IOException;

}
