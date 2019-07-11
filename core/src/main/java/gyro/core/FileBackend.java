package gyro.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

public abstract class FileBackend {

    public abstract Stream<String> list() throws Exception;

    public abstract InputStream openInput(String file) throws Exception;

    public abstract OutputStream openOutput(String file) throws Exception;

}
