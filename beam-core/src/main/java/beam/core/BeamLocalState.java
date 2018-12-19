package beam.core;

import beam.lang.types.ContainerBlock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BeamLocalState extends BeamState {

    @Override
    public ContainerBlock load(String name, BeamCore core) throws IOException {

        File stateFile = new File(name);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            return core.parse(name);
        } else {
            return new ContainerBlock();
        }
    }

    @Override
    public void save(String name, ContainerBlock block) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(name));
            out.write(block.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String name) {

    }

    @Override
    public void execute() {

    }

}
