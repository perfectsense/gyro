package beam.core;

import beam.lang.BeamConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BeamLocalState extends BeamState {

    @Override
    public void load(String name, BeamCore core) {

        File stateFile = new File(name);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            core.processConfig(name);
        }
    }

    @Override
    public void save(String name, BeamConfig state) {
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
