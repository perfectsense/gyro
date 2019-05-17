package gyro.core.resource;

public class FileScope extends Scope {

    private final String file;

    public FileScope(RootScope parent, String file) {
        super(parent);

        this.file = file;
    }

    public String getFile() {
        return file;
    }

}
