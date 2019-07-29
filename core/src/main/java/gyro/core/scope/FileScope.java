package gyro.core.scope;

import com.google.common.base.Preconditions;

public class FileScope extends Scope {

    private final String file;

    public FileScope(RootScope parent, String file) {
        super(parent);

        this.file = Preconditions.checkNotNull(file);
    }

    public String getFile() {
        return file;
    }

}
