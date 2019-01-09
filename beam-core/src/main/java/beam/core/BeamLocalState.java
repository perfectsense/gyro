package beam.core;

import beam.lang.RootNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BeamLocalState extends BeamState {

    @Override
    public RootNode load(RootNode rootNode, BeamCore core) throws IOException {
        String path = rootNode.path().endsWith(".state") ? rootNode.path() : rootNode.path() + ".state";

        RootNode state;

        File stateFile = new File(path);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            state = core.parse(path);
        } else {
            state = new RootNode();
            state.setPath(path);
            state.copyNonResourceState(rootNode);
        }

        return state;
    }

    @Override
    public void save(RootNode rootNode) {
        try {
            String path = rootNode.path().endsWith(".state") ? rootNode.path() : rootNode.path() + ".state";

            BufferedWriter out = new BufferedWriter(new FileWriter(path));
            out.write(rootNode.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String path) {

    }

    @Override
    public void execute() {

    }

}
