package beam.lang;

import beam.lang.types.Value;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.ControlBodyContext;
import beam.parser.antlr4.BeamParser.ExpressionContext;
import beam.parser.antlr4.BeamParser.IfStmtContext;
import beam.parser.antlr4.BeamParser.OperatorContext;

public class IfControl extends Control {

    private BeamVisitor visitor;
    private IfStmtContext context;
    private BeamParser.ControlBodyContext bodyContext;
    private boolean evaluated;

    public IfControl(BeamVisitor visitor, IfStmtContext context) {
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
        if (evaluated()) {
            return;
        }

        bodyContext = evaluateExpression(context.expression(0)) ? context.controlBody(0) : null;
        bodyContext = (bodyContext == null) ? evaluateElseIf() : bodyContext;
        bodyContext = (bodyContext == null && context.ELSE() != null) ? context.controlBody(context.ELSEIF().size() + 1) : bodyContext;

        if (bodyContext != null) {
            Container parent = (Container) parent();
            Frame frame = new Frame();
            frame.parent(parent);
            parent.frames().add(frame);

            for (BeamParser.ControlStmtsContext stmtContext : bodyContext.controlStmts()) {
                if (stmtContext.keyValue() != null) {
                    String key = visitor.parseKey(stmtContext.keyValue().key());
                    Value value = visitor.parseValue(stmtContext.keyValue().value());

                    frame.put(key, value);
                } else if (stmtContext.forStmt() != null) {
                    ForControl forControl = visitor.visitForStmt(stmtContext.forStmt(), frame);
                    frame.putControl(forControl);
                } else if (stmtContext.ifStmt() != null) {
                    IfControl ifControl = visitor.visitIfStmt(stmtContext.ifStmt(), frame);
                    frame.putControl(ifControl);
                } else if (stmtContext.resource() != null) {
                    Resource resource = visitor.visitResource(stmtContext.resource(), frame);
                    frame.putResource(resource);
                } else if (stmtContext.subresource() != null) {
                    Resource resource = visitor.visitSubresource(stmtContext.subresource(), (Resource) parent);
                    frame.putSubresource(resource);
                }
            }
        }

        evaluated(true);
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
        if (expression.AND() != null || expression.OR() != null) {
            ExpressionContext left = expression.expression(0);
            ExpressionContext right = expression.expression(1);

            boolean leftResult = evaluateExpression(left);
            boolean rightResult = evaluateExpression(right);

            return expression.OR() != null ? leftResult || rightResult : leftResult && rightResult;
        } else if (expression.operator() != null) {
            ExpressionContext leftExpression = expression.expression(0);
            ExpressionContext rightExpression = expression.expression(1);

            Value leftValue = visitor.parseValue(leftExpression.value());
            leftValue.parent(parent());
            leftValue.resolve();

            Value rightValue = visitor.parseValue(rightExpression.value());
            rightValue.parent(parent());
            rightValue.resolve();

            Object left = leftValue.getValue();
            Object right = rightValue.getValue();

            OperatorContext operatorContext = expression.operator();
            if (operatorContext.EQ() != null) {
                return left.equals(right);
            } else if (operatorContext.NOTEQ() != null) {
                return !left.equals(right);
            }
        }

        return false;
    }

}
