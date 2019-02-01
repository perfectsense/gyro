package beam.lang.ast.block;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

public class VirtualResourceParamNode extends Node {

    private String name;

    public VirtualResourceParamNode(BeamParser.VirtualResourceParamContext context) {
        name = context.IDENTIFIER().getText();
    }

    public String getName() {
        return name;
    }

    @Override
    public Object evaluate(Scope scope) {
        return name;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {

    }

}
