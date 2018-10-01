package beam;

import beam.commands.SyncCommand;
import beam.commands.UpCommand;
import io.airlift.airline.Cli;
import io.airlift.airline.Help;

public class Beam {

    public static void main(String[] arguments) throws Exception {
        Cli.CliBuilder<Object> builder = Cli.builder("beam").
                withDescription("Beam.").
                withDefaultCommand(Help.class).
                withCommand(UpCommand.class)
                .withCommand(SyncCommand.class);

        Cli<Object> cli = builder.build();

        Object command = cli.parse(arguments);
        if (command instanceof Runnable) {
            ((Runnable) command).run();
        }
    }

}
