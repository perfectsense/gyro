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
import gyro.lang.ast.value.StringExpressionNode;
import gyro.lang.ast.value.StringNode;
import gyro.lang.ast.value.ValueReferenceNode;
import gyro.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class Node {

    public static Node create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(BeamParser.RootContext.class)) {
            return new RootNode((BeamParser.RootContext) context);

        } else if (cc.equals(BeamParser.BooleanValueContext.class)) {
            return new BooleanNode((BeamParser.BooleanValueContext) context);

        } else if (cc.equals(BeamParser.ForStatementContext.class)) {
            return new ForNode((BeamParser.ForStatementContext) context);

        } else if (cc.equals(BeamParser.IfStatementContext.class)) {
            return new IfNode((BeamParser.IfStatementContext) context);

        } else if (cc.equals(BeamParser.VirtualResourceContext.class)) {
            return new VirtualResourceNode((BeamParser.VirtualResourceContext) context);

        } else if (cc.equals(BeamParser.ComparisonConditionContext.class)) {
            return new ComparisonConditionNode((BeamParser.ComparisonConditionContext) context);

        } else if (cc.equals(BeamParser.OrConditionContext.class)) {
            return new OrConditionNode((BeamParser.OrConditionContext) context);

        } else if (cc.equals(BeamParser.AndConditionContext.class)) {
            return new AndConditionNode((BeamParser.AndConditionContext) context);

        } else if (cc.equals(BeamParser.ValueConditionContext.class)) {
            return new ValueConditionNode((BeamParser.ValueConditionContext) context);

        } else if (cc.equals(BeamParser.ValueContext.class)) {
            return Node.create(context.getChild(0));

        } else if (cc.equals(BeamParser.KeyValueStatementContext.class)) {
            return new KeyValueNode((BeamParser.KeyValueStatementContext) context);

        } else if (cc.equals(BeamParser.ImportStmtContext.class)) {
            return new ImportNode((BeamParser.ImportStmtContext) context);

        } else if (cc.equals(BeamParser.ListValueContext.class)) {
            return new ListNode((BeamParser.ListValueContext) context);

        } else if (cc.equals(BeamParser.MapValueContext.class)) {
            return new MapNode((BeamParser.MapValueContext) context);

        } else if (cc.equals(BeamParser.NumberValueContext.class)) {
            return new NumberNode((BeamParser.NumberValueContext) context);

        } else if (cc.equals(BeamParser.ResourceContext.class)) {
            BeamParser.ResourceContext rc = (BeamParser.ResourceContext) context;

            if (rc.resourceName() != null) {
                return new ResourceNode(rc);

            } else {
                String key = rc.resourceType().IDENTIFIER().getText();

                if ("plugin".equals(key)) {
                    return new PluginNode(rc);

                } else {
                    return new KeyBlockNode(rc);
                }
            }

        } else if (cc.equals(BeamParser.ResourceReferenceContext.class)) {
            return new ResourceReferenceNode((BeamParser.ResourceReferenceContext) context);

        } else if (cc.equals(BeamParser.StringExpressionContext.class)) {
            return new StringExpressionNode((BeamParser.StringExpressionContext) context);

        } else if (cc.equals(BeamParser.StringValueContext.class)) {
            BeamParser.StringValueContext svc = (BeamParser.StringValueContext) context;
            BeamParser.StringExpressionContext sec = svc.stringExpression();

            return sec != null
                    ? new StringExpressionNode(sec)
                    : new StringNode(StringUtils.strip(svc.STRING_LITERAL().getText(), "'"));

        } else if (cc.equals(BeamParser.ValueReferenceContext.class)) {
            return new ValueReferenceNode(context.getText());

        } else if (TerminalNode.class.isAssignableFrom(cc)) {
            return new StringNode(context.getText());

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
