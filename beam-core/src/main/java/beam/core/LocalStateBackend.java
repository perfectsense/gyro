package beam.core;

import beam.lang.Resource;
import beam.lang.StateBackend;
import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.RootScope;
import beam.lang.plugins.PluginLoader;

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
    public FileScope load(FileScope scope) throws Exception {
        String path = scope.getFileScope().getPath().endsWith(".state") ? scope.getPath() : scope.getPath() + ".state";

        FileScope state;

        File stateFile = new File(path);
        if (stateFile.exists() && !stateFile.isDirectory()) {
            BeamCore core = new BeamCore();
            state = core.parse(path, true);
        } else {
            state = new RootScope(path);

            state.getFileScope().getPluginLoaders().addAll(scope.getFileScope().getPluginLoaders());
        }

        return state;
    }

    @Override
    public void save(FileScope state) {
        try {
            String path = state.getFileScope().getPath().endsWith(".state") ? state.getPath() : state.getPath() + ".state";

            File temp = File.createTempFile("local-state",".bcl");

            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            for (PluginLoader pluginLoader : state.getFileScope().getPluginLoaders()) {
                out.write(pluginLoader.toString());
            }

            for (Resource resource : state.getFileScope().getResources().values()) {
                out.write(resource.serialize(0));
            }
            out.close();

            File stateFile = new File(path);
            temp.renameTo(stateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String path) {

    }

}
