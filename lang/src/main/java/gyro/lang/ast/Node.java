package gyro.lang.ast;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.lang.ast.condition.AndConditionNode;
import gyro.lang.ast.condition.ComparisonConditionNode;
import gyro.lang.ast.condition.OrConditionNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.control.IfNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.lang.ast.value.ValueReferenceNode;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class Node {

    private static final Map<Class<? extends ParseTree>, Function<ParseTree, Node>> NODE_CONSTRUCTORS = ImmutableMap.<Class<? extends ParseTree>, Function<ParseTree, Node>>builder()
        // ast
        .put(GyroParser.DirectiveContext.class, c -> new DirectiveNode((GyroParser.DirectiveContext) c))
        .put(GyroParser.PairContext.class, c -> new PairNode((GyroParser.PairContext) c))
        // ast.block
        .put(GyroParser.FileContext.class, c -> new FileNode((GyroParser.FileContext) c))
        .put(GyroParser.VirtualResourceContext.class, c -> new VirtualResourceNode((GyroParser.VirtualResourceContext) c))
        // ast.condition
        .put(GyroParser.AndConditionContext.class, c -> new AndConditionNode((GyroParser.AndConditionContext) c))
        .put(GyroParser.ComparisonConditionContext.class, c -> new ComparisonConditionNode((GyroParser.ComparisonConditionContext) c))
        .put(GyroParser.OrConditionContext.class, c -> new OrConditionNode((GyroParser.OrConditionContext) c))
        .put(GyroParser.ValueConditionContext.class, c -> new ValueConditionNode((GyroParser.ValueConditionContext) c))
        // ast.control
        .put(GyroParser.ForStatementContext.class, c -> new ForNode((GyroParser.ForStatementContext) c))
        .put(GyroParser.IfStatementContext.class, c -> new IfNode((GyroParser.IfStatementContext) c))
        // ast.value
        .put(GyroParser.InterpolatedStringContext.class, c -> new InterpolatedStringNode((GyroParser.InterpolatedStringContext) c))
        .put(GyroParser.ListContext.class, c -> new ListNode((GyroParser.ListContext) c))
        .put(GyroParser.MapContext.class, c -> new MapNode((GyroParser.MapContext) c))
        .put(GyroParser.ResourceReferenceContext.class, c -> new ResourceReferenceNode((GyroParser.ResourceReferenceContext) c))
        .put(GyroParser.BooleanValueContext.class, c -> new ValueNode((GyroParser.BooleanValueContext) c))
        .put(GyroParser.NumberContext.class, c -> new ValueNode((GyroParser.NumberContext) c))
        .put(GyroParser.LiteralStringContext.class, c -> new ValueNode((GyroParser.LiteralStringContext) c))
        .put(GyroParser.ValueReferenceContext.class, c -> new ValueReferenceNode((GyroParser.ValueReferenceContext) c))
        .build();

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
        Class<? extends ParseTree> contextClass = context.getClass();

        if (contextClass.equals(GyroParser.ValueContext.class)) {
            return create(context.getChild(0));
        }

        Function<ParseTree, Node> nodeConstructor = NODE_CONSTRUCTORS.get(contextClass);
        Node node;

        if (nodeConstructor != null) {
            node = nodeConstructor.apply(context);

        } else if (contextClass.equals(GyroParser.ResourceContext.class)) {
            GyroParser.ResourceContext rc = (GyroParser.ResourceContext) context;

            if (rc.resourceName() != null) {
                node = new ResourceNode(rc);

            } else {
                node = new KeyBlockNode(rc);
            }

        } else if (TerminalNode.class.isAssignableFrom(contextClass)) {
            node = new ValueNode(context.getText());

        } else {
            throw new GyroLanguageException(String.format(
                "Unrecognized node! [%s]",
                contextClass.getName()));
        }

        if (context instanceof ParserRuleContext) {
            Token token = ((ParserRuleContext) context).getStart();

            node.file = token.getTokenSource().getSourceName();
            node.line = token.getLine();
            node.column = token.getCharPositionInLine();
        }

        return node;
    }

    public abstract <C, R> R accept(NodeVisitor<C, R> visitor, C context);

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
        return NodePrinter.toString(this);
    }

}
