package beam.core;

import beam.lang.BeamFile;
import beam.lang.StateBackend;

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
    public BeamFile load(BeamFile fileNode, BeamCore core) throws IOException {
        String path = fileNode.path().endsWith(".state") ? fileNode.path() : fileNode.path() + ".state";

        BeamFile state;

        File stateFile = new File(path);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            state = core.parse(path);
        } else {
            state = new BeamFile();
            state.setPath(path);
            state.copyNonResourceState(fileNode);
        }

        return state;
    }

    @Override
    public void save(BeamFile fileNode) {
        try {
            String path = fileNode.path().endsWith(".state") ? fileNode.path() : fileNode.path() + ".state";

            BufferedWriter out = new BufferedWriter(new FileWriter(path));
            out.write(fileNode.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String path) {

    }

}
