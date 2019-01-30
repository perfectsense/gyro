package beam.lang.ast.scope;

import beam.core.LocalStateBackend;
import beam.lang.Resource;
import beam.lang.StateBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileScope extends Scope {

    private FileScope state;
    private List<FileScope> imports = new ArrayList<>();

    public FileScope(Scope parent, Map<String, Object> values) {
        super(parent, values);
    }

    public FileScope(Scope parent) {
        super(parent);
    }

    public FileScope getState() {
        if (state == null) {
            state = new FileScope(null);
            state.setPath(getPath());
        }

        return state;
    }

    public void setState(FileScope state) {
        this.state = state;
    }

    public List<FileScope> getImports() {
        return imports;
    }

    public StateBackend getStateBackend() {
        return new LocalStateBackend();
    }

    public void setStateBackend(Resource resource) {
        getTop().put("_state_backend", resource);
    }

    public String getPath() {
        return (String) get("_file");
    }

    public void setPath(String path) {
        put("_file", path);
    }

}
