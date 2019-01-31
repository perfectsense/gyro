package beam.core;

import beam.lang.Resource;
import beam.lang.StateBackend;
import beam.lang.ast.Node;
import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.RootScope;
import beam.lang.plugins.PluginLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalStateBackend extends StateBackend {

    @Override
    public String name() {
        return "local";
    }

    @Override
    public FileScope load(FileScope parent, String file) throws Exception {
        if (!file.endsWith(".bcl") && !file.endsWith(".bcl.state")) {
            file += ".bcl";
        }

        Path filePath = parent != null
                ? Paths.get(parent.getFile()).getParent().resolve(file)
                : Paths.get(file);

        file = filePath.toString();

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            Node node = Node.parse(file);

            FileScope scope = parent != null
                    ? new FileScope(parent, file)
                    : new RootScope(file);

            node.evaluate(scope);
            return scope;

        } else {
            FileScope state = new RootScope(file);
            // state.getFileScope().getPluginLoaders().addAll(parent.getPluginLoaders());
            return state;
        }
    }

    @Override
    public void save(FileScope state) {
        try {
            String path = state.getFileScope().getFile().endsWith(".state") ? state.getFile() : state.getFile() + ".state";

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
