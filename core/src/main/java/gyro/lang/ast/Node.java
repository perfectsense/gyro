package gyro.lang.ast;

import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.PluginNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.block.RootNode;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.control.IfNode;
import gyro.lang.ast.expression.AndNode;
import gyro.lang.ast.expression.ComparisonNode;
import gyro.lang.ast.expression.OrNode;
import gyro.lang.ast.expression.ValueExpressionNode;
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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class Node {

    private NodeLocation location;

    public NodeLocation getLocation() {
        return location;
    }

    public static Node create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();
        Node node;

        if (cc.equals(BeamParser.BeamFileContext.class)) {
            node = new RootNode((BeamParser.BeamFileContext) context);

        } else if (cc.equals(BeamParser.BooleanValueContext.class)) {
            node = new BooleanNode((BeamParser.BooleanValueContext) context);

        } else if (cc.equals(BeamParser.ForStmtContext.class)) {
            node = new ForNode((BeamParser.ForStmtContext) context);

        } else if (cc.equals(BeamParser.IfStmtContext.class)) {
            node = new IfNode((BeamParser.IfStmtContext) context);

        } else if (cc.equals(BeamParser.VirtualResourceContext.class)) {
            node = new VirtualResourceNode((BeamParser.VirtualResourceContext) context);

        } else if (cc.equals(BeamParser.ComparisonExpressionContext.class)) {
            node = new ComparisonNode((BeamParser.ComparisonExpressionContext) context);

        } else if (cc.equals(BeamParser.OrExpressionContext.class)) {
            node = new OrNode((BeamParser.OrExpressionContext) context);

        } else if (cc.equals(BeamParser.AndExpressionContext.class)) {
            node = new AndNode((BeamParser.AndExpressionContext) context);

        } else if (cc.equals(BeamParser.ValueExpressionContext.class)) {
            node = new ValueExpressionNode((BeamParser.ValueExpressionContext) context);

        } else if (cc.equals(BeamParser.ValueContext.class)) {
            node = Node.create(context.getChild(0));

        } else if (cc.equals(BeamParser.KeyValueContext.class)) {
            node = new KeyValueNode((BeamParser.KeyValueContext) context);

        } else if (cc.equals(BeamParser.ImportStmtContext.class)) {
            node = new ImportNode((BeamParser.ImportStmtContext) context);

        } else if (cc.equals(BeamParser.ListValueContext.class)) {
            node = new ListNode((BeamParser.ListValueContext) context);

        } else if (cc.equals(BeamParser.MapValueContext.class)) {
            node = new MapNode((BeamParser.MapValueContext) context);

        } else if (cc.equals(BeamParser.NumberValueContext.class)) {
            node = new NumberNode((BeamParser.NumberValueContext) context);

        } else if (cc.equals(BeamParser.ReferenceBodyContext.class)) {
            BeamParser.ReferenceBodyContext rbc = (BeamParser.ReferenceBodyContext) context;
            String type = rbc.referenceType().getText();

            if (type.contains("::")) {
                node = new ResourceReferenceNode(rbc);

            } else {
                node = new ValueReferenceNode(type);
            }

        } else if (cc.equals(BeamParser.ReferenceValueContext.class)) {
            node = create(((BeamParser.ReferenceValueContext) context).referenceBody());

        } else if (cc.equals(BeamParser.ResourceContext.class)) {
            BeamParser.ResourceContext rc = (BeamParser.ResourceContext) context;

            if (rc.resourceName() != null) {
                node = new ResourceNode(rc);

            } else {
                String key = rc.resourceType().IDENTIFIER().getText();

                if ("plugin".equals(key)) {
                    node = new PluginNode(rc);

                } else {
                    node = new KeyBlockNode(rc);
                }
            }

        } else if (cc.equals(BeamParser.StringExpressionContext.class)) {
            node = new StringExpressionNode((BeamParser.StringExpressionContext) context);

        } else if (cc.equals(BeamParser.StringValueContext.class)) {
            BeamParser.StringValueContext svc = (BeamParser.StringValueContext) context;
            BeamParser.StringExpressionContext sec = svc.stringExpression();

            node = sec != null
                    ? new StringExpressionNode(sec)
                    : new StringNode(StringUtils.strip(svc.STRING_LITERAL().getText(), "'"));

        } else if (TerminalNode.class.isAssignableFrom(cc)) {
            node = new StringNode(context.getText());

        } else {
            node = new UnknownNode(context);
        }

        String file = null;
        Integer line = null;
        Integer column = null;
        if (context instanceof ParserRuleContext) {
            ParserRuleContext parserRuleContext = (ParserRuleContext) context;
            file = parserRuleContext.getStart().getTokenSource().getSourceName();
            line = parserRuleContext.getStart().getLine();
            column = parserRuleContext.getStart().getCharPositionInLine();
        }

        node.location = new NodeLocation(file, line, column);
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildString(builder, 0);
        return builder.toString();
    }
}
