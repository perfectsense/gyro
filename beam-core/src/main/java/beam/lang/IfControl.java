package beam.lang;

import beam.core.BeamCore;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.ControlBodyContext;
import beam.parser.antlr4.BeamParser.ExpressionContext;
import beam.parser.antlr4.BeamParser.IfStmtContext;

public class IfControl extends Control {

    private BeamCore core;
    private BeamVisitor visitor;
    private IfStmtContext context;
    private BeamParser.ControlBodyContext bodyContext;
    private boolean evaluated;

    public IfControl(BeamCore core, BeamVisitor visitor, IfStmtContext context) {
        this.core = core;
        this.visitor = visitor;
        this.context = context;
    }

    @Override
    public boolean resolve() {
        evaluate();

        return true;
    }

    @Override
    public void evaluate() {
    }

    private ControlBodyContext evaluateElseIf() {
        for (int i = 1; i < context.ELSEIF().size() + 1; i++) {
            if (evaluateExpression(context.expression(i))) {
                return context.controlBody(i);
            }
        }

        return null;
    }

    private boolean evaluated() {
        return evaluated;
    }

    private void evaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    private boolean evaluateExpression(ExpressionContext expression) {
        return false;
    }

}
