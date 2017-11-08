package beam.cli;

import beam.Beam;
import beam.BeamException;
import beam.pkg.BasePackagePlugin;
import beam.pkg.BeamPackageFetcher;
import com.psddev.dari.util.IoUtils;
import groovy.lang.GroovyClassLoader;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.ParseArgumentsUnexpectedException;
import io.airlift.command.ParseOptionMissingException;
import io.airlift.command.SingleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Command(name = "init", description = "Initialize a beam package.")
public class InitCommand extends AbstractCommand {

    @Arguments(title = "packageUrl", required = true)
    public String packageUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(InitCommand.class);

    @Override
    protected void doExecute() throws Exception {
        String beamPath = System.getProperty("user.home") + "/.beam";
        final String packagesPath = beamPath + "/packages";

        new File(beamPath).mkdirs();
        new File(packagesPath).mkdirs();

        BasePackagePlugin plugin = new BasePackagePlugin();
        BeamPackageFetcher fetch = getPackageFetcher(packageUrl);
        Path packagePath = fetch.fetchPackage(packageUrl, System.getProperty("user.home") + "/.beam/packages");

        Path packagePluginPath = FileSystems.getDefault().getPath(packagePath.toString(), "_plugin_", "Plugin.groovy");
        if (packagePluginPath.toFile().exists()) {
            ClassLoader parent = getClass().getClassLoader();
            GroovyClassLoader loader = new GroovyClassLoader(parent);
            Class groovyClass = loader.parseClass(packagePluginPath.toFile());

            plugin = (BasePackagePlugin) groovyClass.newInstance();
        }

        // Parse options.
        List<String> options = getUnparsedArgument();
        try {
            options.remove("init");
            options.remove("-debug");
            options.remove(packageUrl);

            SingleCommand c = SingleCommand.singleCommand(plugin.getClass());
            plugin = (BasePackagePlugin) c.parse(options);
        } catch(ParseOptionMissingException pome) {
            throw new BeamException("This package requires a parameter that was missing. " + pome.getMessage());
        } catch(ParseArgumentsUnexpectedException paue) {
            throw new BeamException("This package does not support a provided parameter. " + paue.getMessage());
        }

        final Path templatesPath = packagePath;
        final BasePackagePlugin packagePlugin = plugin;

        packagePlugin.beforeProcessing(packagePath);

        Files.walkFileTree(packagePath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String template = IoUtils.toString(file.toFile(), Charset.forName("UTF-8"));
                String outputFilename = packagePlugin.processFilename(file.toString().replaceFirst(templatesPath.toString(), "."));

                template = packagePlugin.processTemplate(file.toString(), template);
                if (template == null) {
                    return FileVisitResult.CONTINUE;
                }

                System.out.println("Processing " + outputFilename + "...");

                File outputFile = new File(outputFilename);
                FileWriter writer = new FileWriter(outputFile);
                writer.write(template);
                writer.close();

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String directory = packagePlugin.processDirectoryName(dir.toString().replace(templatesPath.toString(), "."));
                if (directory.toString().contains(".git") || directory.toString().startsWith("./_plugin_")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                File templateFile = new File(directory);
                templateFile.mkdirs();

                return FileVisitResult.CONTINUE;
            }

        });

        packagePlugin.afterProcessing(packagePath);
    }

    /**
     * @return Never {@code null}.
     */
    public static BeamPackageFetcher getPackageFetcher(String packageUrl) {
        for (Class<? extends BeamPackageFetcher> creatorClass : Beam.getReflections().getSubTypesOf(BeamPackageFetcher.class)) {
            try {
                BeamPackageFetcher fetcher = creatorClass.newInstance();

                try {
                    if (fetcher.canFetchPackage(packageUrl)) {
                        return fetcher;
                    }

                } catch (Exception error) {
                    LOGGER.debug(String.format(
                                    "Can't create an package fetcher using [%s]!",
                                    creatorClass.getName()),
                            error);
                }

            } catch (IllegalAccessException | InstantiationException error) {
                LOGGER.warn(String.format(
                                "Can't create an instance of [%s]!",
                                creatorClass.getName()),
                        error);
            }
        }

        throw new BeamException("Can't find fetcher for url: " + packageUrl);
    }

}