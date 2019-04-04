package gyro.lang.ast.block;

import gyro.parser.antlr4.BeamParser;

public class VirtualResourceParameter {

    private final String name;

    public VirtualResourceParameter(BeamParser.VirtualResourceParameterContext context) {
        name = context.getText();
    }

    public String getName() {
        return name;
    }

}
