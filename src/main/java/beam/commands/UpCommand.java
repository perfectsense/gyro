package beam.commands;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import beam.core.BeamConfigLocation;
import beam.core.BeamException;
import beam.core.BeamRuntime;
import beam.core.diff.ChangeType;
import beam.core.diff.DiffUtil;
import beam.core.diff.ResourceDiff;
import beam.parser.ASTListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import beam.parser.ast.Node;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import org.fusesource.jansi.AnsiRenderWriter;

@Command(name = "up", description = "Updates all assets to match the configuration.")
public class UpCommand implements Runnable {

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public boolean debug;

    public void run() {
        try {
            File configFile = File.createTempFile("config", ".beam");
            configFile.deleteOnExit();
            File dir = new File("legacy");
            StringBuilder sb = new StringBuilder();
            sb.append(new Scanner(new File("test2.beam")).useDelimiter("\\Z").next());
            if (dir.isDirectory()) {
                File[] directoryListing = dir.listFiles();
                if (directoryListing != null) {
                    for (File legacyConfig : directoryListing) {
                        sb.append(new Scanner(legacyConfig).useDelimiter("\\Z").next());
                    }
                }
            }

            PrintWriter configOut = new PrintWriter(configFile);
            configOut.println(sb.toString() + "\n");
            configOut.close();

            ASTBeamRoot root = parse(configFile.getCanonicalPath());

            if (root == null) {
                return;
            }

            ResourceDiff resourceDiff = root.getDiff();
            PrintWriter out = new AnsiRenderWriter(System.out, true);
            Set<ChangeType> changeTypes = new HashSet<>();
            BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));
            try {

                changeTypes.clear();
                DiffUtil.writeDiffs(Arrays.asList(resourceDiff), 0, out, changeTypes);

                boolean hasChanges = false;
                if (changeTypes.contains(ChangeType.CREATE) || changeTypes.contains(ChangeType.UPDATE)) {

                    out.format("\nAre you sure you want to create and/or update resources? (y/N) ");
                    out.flush();
                    hasChanges = true;

                    if ("y".equalsIgnoreCase(confirmReader.readLine())) {
                        out.println("");
                        out.flush();
                        DiffUtil.setChangeable(Arrays.asList(resourceDiff));
                        DiffUtil.createOrUpdate(Arrays.asList(resourceDiff), out);
                    }
                }

                boolean delete = true;
                if (changeTypes.contains(ChangeType.DELETE)) {
                    hasChanges = true;

                    if (delete) {
                        out.format("\nAre you sure you want to delete resources? (y/N) ");
                        out.flush();

                        if ("y".equalsIgnoreCase(confirmReader.readLine())) {

                            out.println("");
                            out.flush();
                            DiffUtil.setChangeable(Arrays.asList(resourceDiff));
                            DiffUtil.delete(Arrays.asList(resourceDiff), out);
                        }

                    } else {
                        out.format("\nSkipped deletes. Run again with the --delete option to execute them.\n");
                        out.flush();
                    }
                }

                if (!hasChanges) {
                    out.format("\nNo changes.\n");
                    out.flush();
                }

            } catch (Exception e) {
                throw new BeamException(e.getMessage(), e.getCause());
            } finally {
                if (!BeamRuntime.getBeamConfigLocations().isEmpty()) {
                    try {
                        File tempConfig = File.createTempFile("beam_back_fill", ".beam");
                        tempConfig.deleteOnExit();
                        FileOutputStream outputStream = new FileOutputStream(tempConfig);

                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

                        // if config locations is empty don't do the copy
                        File currentConfig = new File("test2.beam");
                        BufferedReader br = new BufferedReader(new FileReader(currentConfig));

                        String line = null;
                        int count = 0;
                        while ((line = br.readLine()) != null) {
                            count++;
                            for (BeamConfigLocation location : BeamRuntime.getBeamConfigLocations()) {
                                if (location.getLine() == count) {
                                    for (String key : location.getContentMap().keySet()) {
                                        for (int i = 0; i < location.getColumn(); i++) {
                                            bufferedWriter.write(" ");
                                        }

                                        bufferedWriter.write(String.format("%s: \"%s\"", key, location.getContentMap().get(key)));
                                        bufferedWriter.newLine();
                                    }
                                }
                            }

                            bufferedWriter.write(line);
                            bufferedWriter.newLine();
                        }

                        br.close();
                        bufferedWriter.close();

                        tempConfig.renameTo(currentConfig);
                    } catch (Exception e) {
                        throw new BeamException("Unable to back fill resource ids", e);
                    }
                }
            }

            for (Node n : root.getNodes()) {
                System.out.println(n);
            }
        } catch (IOException ioe) {

        }
    }

    public ASTBeamRoot parse(String filename) {
        try {


            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();


            long startTime = System.currentTimeMillis();
            ASTListener listener = new ASTListener();
            ParseTreeWalker.DEFAULT.walk(listener, context);

            long endTime = System.currentTimeMillis();
            System.out.println("walk took " + (endTime - startTime)/1000.0 + " seconds");
            return listener.getRoot();

        } catch (Exception error) {
            Throwable cause = null;

            PrintWriter out = new AnsiRenderWriter(System.out, true);
            if (error instanceof BeamException) {
                out.write("\n@|red Error: " + error.getMessage() + "|@\n");
                out.flush();

                if (debug) {
                    cause = error.getCause();
                }

            } else {
                out.write("\n@|red Unexpected error! Stack trace follows:|@\n");
                out.flush();

                cause = error;
            }

            if (cause != null) {
                out.write(cause.getClass().getName());
                out.write(": ");
                cause.printStackTrace(out);
                out.flush();
            }
        }

        return null;
    }
}
