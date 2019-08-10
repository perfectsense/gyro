package gyro.core;

import gyro.core.scope.RootScope;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

public abstract class FileBackend {

    private String name;
    private RootScope rootScope;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RootScope getRootScope() {
        return rootScope;
    }

    public void setRootScope(RootScope rootScope) {
        this.rootScope = rootScope;
    }

    public abstract Stream<String> list() throws Exception;

    public abstract InputStream openInput(String file) throws Exception;

    public abstract OutputStream openOutput(String file) throws Exception;

    public abstract void delete(String file) throws Exception;

}
