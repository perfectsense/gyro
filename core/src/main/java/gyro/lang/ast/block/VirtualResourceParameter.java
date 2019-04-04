package gyro.lang.ast.block;

import gyro.parser.antlr4.GyroParser;

public class VirtualResourceParameter {

    private final String name;

    public VirtualResourceParameter(GyroParser.VirtualResourceParameterContext context) {
        name = context.getText();
    }

    public String getName() {
        return name;
    }

}
