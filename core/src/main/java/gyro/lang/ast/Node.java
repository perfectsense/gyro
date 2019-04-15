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
import gyro.core.scope.Scope;
import gyro.lang.ast.value.BooleanNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.NumberNode;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.LiteralStringNode;
import gyro.lang.ast.value.ValueReferenceNode;
import org.antlr.v4.runtime.ParserRuleContext;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

public abstract class Node {

    private String file;
    private Integer line;
    private Integer column;

    public String getFile() {
        return file;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }

    public static Node create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();
        Node node;

        if (cc.equals(GyroParser.RootContext.class)) {
            node = new RootNode((GyroParser.RootContext) context);

        } else if (cc.equals(GyroParser.BooleanValueContext.class)) {
            node = new BooleanNode((GyroParser.BooleanValueContext) context);

        } else if (cc.equals(GyroParser.ForStatementContext.class)) {
            node = new ForNode((GyroParser.ForStatementContext) context);

        } else if (cc.equals(GyroParser.IfStatementContext.class)) {
            node = new IfNode((GyroParser.IfStatementContext) context);

        } else if (cc.equals(GyroParser.VirtualResourceContext.class)) {
            node = new VirtualResourceNode((GyroParser.VirtualResourceContext) context);

        } else if (cc.equals(GyroParser.ComparisonConditionContext.class)) {
            node = new ComparisonConditionNode((GyroParser.ComparisonConditionContext) context);

        } else if (cc.equals(GyroParser.OrConditionContext.class)) {
            node = new OrConditionNode((GyroParser.OrConditionContext) context);

        } else if (cc.equals(GyroParser.AndConditionContext.class)) {
            node = new AndConditionNode((GyroParser.AndConditionContext) context);

        } else if (cc.equals(GyroParser.ValueConditionContext.class)) {
            node = new ValueConditionNode((GyroParser.ValueConditionContext) context);

        } else if (cc.equals(GyroParser.ValueContext.class)) {
            node = Node.create(context.getChild(0));

        } else if (cc.equals(GyroParser.PairContext.class)) {
            node = new PairNode((GyroParser.PairContext) context);

        } else if (cc.equals(GyroParser.ListContext.class)) {
            node = new ListNode((GyroParser.ListContext) context);

        } else if (cc.equals(GyroParser.MapContext.class)) {
            node = new MapNode((GyroParser.MapContext) context);

        } else if (cc.equals(GyroParser.NumberContext.class)) {
            node = new NumberNode((GyroParser.NumberContext) context);

        } else if (cc.equals(GyroParser.ResourceContext.class)) {
            GyroParser.ResourceContext rc = (GyroParser.ResourceContext) context;

            if (rc.resourceName() != null) {
                node = new ResourceNode(rc);

            } else {
                String key = rc.resourceType().getText();

                if ("plugin".equals(key)) {
                    node = new PluginNode(rc);

                } else {
                    node = new KeyBlockNode(rc);
                }
            }

        } else if (cc.equals(GyroParser.ResourceReferenceContext.class)) {
            node = new ResourceReferenceNode((GyroParser.ResourceReferenceContext) context);

        } else if (cc.equals(GyroParser.LiteralStringContext.class)) {
            node = new LiteralStringNode((GyroParser.LiteralStringContext) context);

        } else if (cc.equals(GyroParser.InterpolatedStringContext.class)) {
            node = new InterpolatedStringNode((GyroParser.InterpolatedStringContext) context);

        } else if (cc.equals(GyroParser.ValueReferenceContext.class)) {
            node = new ValueReferenceNode((GyroParser.ValueReferenceContext) context);

        } else if (TerminalNode.class.isAssignableFrom(cc)) {
            node = new LiteralStringNode(context.getText());

        } else {
            node = new UnknownNode(context);
        }

        if (context instanceof ParserRuleContext) {
            ParserRuleContext parserRuleContext = (ParserRuleContext) context;
            node.file = parserRuleContext.getStart().getTokenSource().getSourceName();
            node.line = parserRuleContext.getStart().getLine();
            node.column = parserRuleContext.getStart().getCharPositionInLine();
        }

        return node;
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

    public String getLocation() {
        StringBuilder sb = new StringBuilder();
        if (file != null) {
            sb.append("in ");
            sb.append(file);
            sb.append(" ");
        }

        if (line != null) {
            sb.append("on line ");
            sb.append(line);
            sb.append(" ");
        }

        if (column != null) {
            sb.append("at column ");
            sb.append(column);
            sb.append(" ");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            sb.append(": ");
        }

        return sb.toString();
    }

    public String deferFailure() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildString(builder, 0);
        return builder.toString();
    }
}
