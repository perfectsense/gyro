package beam.plugins.ssh;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.BeamInstance;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.inject.Inject;

@Command(name = "ssh", description = "SSH to a running instance.")
public class SshCommand extends AbstractInstanceCommand {

    private static final Table SSH_TABLE = new Table()
        .addColumn("#", 3)
        .addColumn("Instance ID", 20)
        .addColumn("Name", 35)
        .addColumn("State", 12)
        .addColumn("Hostname", 65);

    @Option(name = { "-e", "--execute" }, description = "Command to execute on host(s).")
    public String command;

    @Option(name = { "-c", "--continue" }, description = "Ignore exit code and continue running -e command to run on all hosts.")
    public boolean force;

    @Option(name = { "--tmux" }, description = "Open a tmux session with each host.")
    public boolean useTmux;

    @Inject
    public SshOptions sshOptions;

    public String command() {
        return command;
    }

    public boolean force() {
        return force;
    }

    public boolean useTmux() {
        return useTmux;
    }

    @Override
    public void doExecute(List<BeamInstance> instances) throws Exception {
        if (command != null) {
            for (BeamInstance instance : instances) {
                BeamCore.ui().write("Executing @|green %s|@ on @|yellow %s|@ %s...\n", command, instance.getHostname());

                int exitCode = SshOptions.createProcessBuilder(sshOptions, instance, command).inheritIO().start().waitFor();

                if (exitCode != 0 && !force) {
                    BeamCore.ui().write("@|red Command failed!|@\n");
                    return;
                }
            }
        } else if (useTmux) {
            String tmuxScript = "#!/bin/sh\n";
            tmuxScript += "SESSION=`tmux new-session -d -P`\n";

            for (BeamInstance instance : instances) {
                List<String> arguments = SshOptions.createArgumentsList(sshOptions, instance);

                String sshCommand = "";
                for (String arg : arguments) {
                    if (arg.contains(" ")) {
                        sshCommand += "\'" + arg + "\'";
                    } else {
                        sshCommand += arg;
                    }

                    sshCommand += " ";
                }

                tmuxScript += "tmux new-window -t ${SESSION}" + " -n " + instance.getInstanceId() + " -- " + sshCommand + "\n";
            }

            tmuxScript += "tmux kill-window -t ${SESSION}1\n";
            tmuxScript += "tmux move-window -rt ${SESSION}\n";
            tmuxScript += "tmux attach-session -t ${SESSION}\n";

            File temp = File.createTempFile("tmux", "beam");
            temp.setExecutable(true);
            temp.deleteOnExit();

            Writer out = new FileWriter(temp);
            out.write(tmuxScript);
            out.close();

            new ProcessBuilder(temp.toString()).inheritIO().start().waitFor();
        } else if (instances.size() == 1) {
            BeamInstance instance = instances.get(0);

            SshOptions.createProcessBuilder(sshOptions, instance)
                .inheritIO()
                .start()
                .waitFor();

        } else {
            BeamInstance instance = pickInstance(instances);

            SshOptions.createProcessBuilder(sshOptions, instance)
                .inheritIO()
                .start()
                .waitFor();
        }

    }

    static BeamInstance pickInstance(List<BeamInstance> instances) throws IOException {
        SSH_TABLE.writeHeader(BeamCore.ui());

        int index = 0;

        for (BeamInstance instance : instances) {
            ++ index;

            SSH_TABLE.writeRow(
                BeamCore.ui(),
                index,
                instance.getInstanceId(),
                instance.getName(),
                instance.getState(),
                instance.getHostname());
        }

        SSH_TABLE.writeFooter(BeamCore.ui());

        int pick = ObjectUtils.to(int.class, BeamCore.ui().readText("\nMore than one instance matched your criteria, pick one to log into: "));

        if (pick > instances.size() || pick <= 0) {
            throw new BeamException(String.format("Must pick a number between 1 and %d!", instances.size()));
        }

        return instances.get(pick - 1);
    }

}
