package beam.core;

import beam.lang.BeamBlock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BeamLocalState extends BeamState {

    @Override
    public BeamBlock load(String name, BeamCore core) throws IOException {

        File stateFile = new File(name);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            return core.parse(name);
        } else {
            return new BeamBlock();
        }
    }

    @Override
    public void save(String name, BeamBlock state) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(name));
            out.write(state.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String name) {

    }

}
