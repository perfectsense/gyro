package gyro.core.command;

import java.util.concurrent.Callable;

import picocli.CommandLine;

public abstract class AbstractDynamicCommand implements Callable<Integer>, GyroCommand {

    private CommandLine.ParseResult parseResult;

    public CommandLine.ParseResult getParseResult() {
        return parseResult;
    }

    public void setParseResult(CommandLine.ParseResult parseResult) {
        this.parseResult = parseResult;
    }
}
