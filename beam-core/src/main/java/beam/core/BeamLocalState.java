package beam.core;

import beam.lang.BeamConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BeamLocalState extends BeamState {

    @Override
    public BeamResource load(String name) {
        return null;
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
