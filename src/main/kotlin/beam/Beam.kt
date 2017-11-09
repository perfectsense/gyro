package beam

import beam.commands.UpCommand
import io.airlift.airline.Cli
import io.airlift.airline.Help

class Beam(args: Array<String>) : Runnable {

    lateinit var cli: Cli<Any>;

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val commands = listOf(UpCommand::class.java, Help::class.java)

            val builder = Cli.builder<Any>("beam")
                    .withDescription("Beam.")
                    .withDefaultCommand(Help::class.java)
                    .withCommands(commands.asIterable())

            cli = builder.build();

            val command = cli.parse(args.asIterable())

            if (command is Runnable) {
                command.run();
            }
        }

    }

    override fun run() {
    }

}
