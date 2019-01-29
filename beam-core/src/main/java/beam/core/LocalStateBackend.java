package beam.core;

import beam.lang.BeamFile;
import beam.lang.StateBackend;
import beam.lang.ast.Scope;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LocalStateBackend extends StateBackend {

    @Override
    public String name() {
        return "local";
    }

    @Override
    public BeamFile load(BeamFile fileNode) throws IOException {
        String path = fileNode.path().endsWith(".state") ? fileNode.path() : fileNode.path() + ".state";

        BeamFile state;

        File stateFile = new File(path);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            BeamCore core = new BeamCore();
            state = core.parse(path, true);
        } else {
            state = new BeamFile();
            state.path(path);
            state.copyNonResourceState(fileNode);
        }

        return state;
    }

    @Override
    public Scope load(Scope scope) throws Exception {
        String path = scope.getPath().endsWith(".state") ? scope.getPath() : scope.getPath() + ".state";

        Scope state;

        File stateFile = new File(path);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            BeamCore core = new BeamCore();
            state = core.parseScope(path, true);
        } else {
            state = new Scope(null);
            state.setPath(path);
        }

        return state;
    }

    @Override
    public void save(BeamFile fileNode) {
        try {
            String path = fileNode.path().endsWith(".state") ? fileNode.path() : fileNode.path() + ".state";

            BufferedWriter out = new BufferedWriter(new FileWriter(path));
            out.write(fileNode.serialize(0));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(Scope state) {

    }

    @Override
    public void delete(String path) {

    }

}
