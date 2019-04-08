package gyro.lang.ast;

import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.PluginNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.block.RootNode;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.control.IfNode;
import gyro.lang.ast.condition.AndConditionNode;
import gyro.lang.ast.condition.ComparisonConditionNode;
import gyro.lang.ast.condition.OrConditionNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ast.value.BooleanNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.NumberNode;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.LiteralStringNode;
import gyro.lang.ast.value.ValueReferenceNode;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

public abstract class Node {

    public static Node create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(GyroParser.RootContext.class)) {
            return new RootNode((GyroParser.RootContext) context);

        } else if (cc.equals(GyroParser.BooleanValueContext.class)) {
            return new BooleanNode((GyroParser.BooleanValueContext) context);

        } else if (cc.equals(GyroParser.ForStatementContext.class)) {
            return new ForNode((GyroParser.ForStatementContext) context);

        } else if (cc.equals(GyroParser.IfStatementContext.class)) {
            return new IfNode((GyroParser.IfStatementContext) context);

        } else if (cc.equals(GyroParser.VirtualResourceContext.class)) {
            return new VirtualResourceNode((GyroParser.VirtualResourceContext) context);

        } else if (cc.equals(GyroParser.ComparisonConditionContext.class)) {
            return new ComparisonConditionNode((GyroParser.ComparisonConditionContext) context);

        } else if (cc.equals(GyroParser.OrConditionContext.class)) {
            return new OrConditionNode((GyroParser.OrConditionContext) context);

        } else if (cc.equals(GyroParser.AndConditionContext.class)) {
            return new AndConditionNode((GyroParser.AndConditionContext) context);

        } else if (cc.equals(GyroParser.ValueConditionContext.class)) {
            return new ValueConditionNode((GyroParser.ValueConditionContext) context);

        } else if (cc.equals(GyroParser.ValueContext.class)) {
            return Node.create(context.getChild(0));

        } else if (cc.equals(GyroParser.PairContext.class)) {
            return new PairNode((GyroParser.PairContext) context);

        } else if (cc.equals(GyroParser.DirectiveContext.class)) {
            return new DirectiveNode((GyroParser.DirectiveContext) context);

        } else if (cc.equals(GyroParser.ListContext.class)) {
            return new ListNode((GyroParser.ListContext) context);

        } else if (cc.equals(GyroParser.MapContext.class)) {
            return new MapNode((GyroParser.MapContext) context);

        } else if (cc.equals(GyroParser.NumberContext.class)) {
            return new NumberNode((GyroParser.NumberContext) context);

        } else if (cc.equals(GyroParser.ResourceContext.class)) {
            GyroParser.ResourceContext rc = (GyroParser.ResourceContext) context;

            if (rc.resourceName() != null) {
                return new ResourceNode(rc);

            } else {
                String key = rc.resourceType().getText();

                if ("plugin".equals(key)) {
                    return new PluginNode(rc);

                } else {
                    return new KeyBlockNode(rc);
                }
            }

        } else if (cc.equals(GyroParser.ResourceReferenceContext.class)) {
            return new ResourceReferenceNode((GyroParser.ResourceReferenceContext) context);

        } else if (cc.equals(GyroParser.LiteralStringContext.class)) {
            return new LiteralStringNode((GyroParser.LiteralStringContext) context);

        } else if (cc.equals(GyroParser.InterpolatedStringContext.class)) {
            return new InterpolatedStringNode((GyroParser.InterpolatedStringContext) context);

        } else if (cc.equals(GyroParser.ValueReferenceContext.class)) {
            return new ValueReferenceNode((GyroParser.ValueReferenceContext) context);

        } else if (TerminalNode.class.isAssignableFrom(cc)) {
            return new LiteralStringNode(context.getText());

        } else {
            return new UnknownNode(context);
        }
    }

    public abstract Object evaluate(Scope scope) throws Exception;

    public abstract void buildString(StringBuilder builder, int indentDepth);

    protected void buildNewline(StringBuilder builder, int indentDepth) {
        builder.append('\n');

        for (int i = 0; i < indentDepth; i++) {
            builder.append("    ");
        }
    }

    protected void buildBody(StringBuilder builder, int indentDepth, List<Node> body) {
        for (Node n : body) {
            buildNewline(builder, indentDepth);
            n.buildString(builder, indentDepth);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildString(builder, 0);
        return builder.toString();
    }
}
