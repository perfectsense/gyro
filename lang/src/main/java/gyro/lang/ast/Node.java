package gyro.lang.ast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.lang.GyroErrorListener;
import gyro.lang.GyroErrorStrategy;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.BinaryNode;
import gyro.lang.ast.value.IndexedNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroLexer;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class Node {

    private static final Function<ParseTree, Node> GET_FIRST_CHILD = c -> Node.create(c.getChild(0));

    private static final Map<Class<? extends ParseTree>, Function<ParseTree, Node>> NODE_CONSTRUCTORS = ImmutableMap.<Class<? extends ParseTree>, Function<ParseTree, Node>>builder()
        .put(GyroParser.BareStringContext.class, c -> new ValueNode((GyroParser.BareStringContext) c))
        .put(GyroParser.BoolContext.class, c -> new ValueNode((GyroParser.BoolContext) c))
        .put(GyroParser.DirectiveContext.class, c -> new DirectiveNode((GyroParser.DirectiveContext) c))
        .put(GyroParser.FileContext.class, c -> new FileNode((GyroParser.FileContext) c))
        .put(GyroParser.GroupedMulItemContext.class, c -> Node.create(((GyroParser.GroupedMulItemContext) c).value()))
        .put(GyroParser.IndexContext.class, GET_FIRST_CHILD)
        .put(GyroParser.IndexedMulItemContext.class, c -> new IndexedNode((GyroParser.IndexedMulItemContext) c))
        .put(GyroParser.InterpolatedStringContext.class, c -> new InterpolatedStringNode((GyroParser.InterpolatedStringContext) c))
        .put(GyroParser.ItemContext.class, GET_FIRST_CHILD)
        .put(GyroParser.KeyBlockContext.class, c -> new KeyBlockNode((GyroParser.KeyBlockContext) c))
        .put(GyroParser.KeyContext.class, GET_FIRST_CHILD)
        .put(GyroParser.ListContext.class, c -> new ListNode((GyroParser.ListContext) c))
        .put(GyroParser.LiteralStringContext.class, c -> new ValueNode((GyroParser.LiteralStringContext) c))
        .put(GyroParser.MapContext.class, c -> new MapNode((GyroParser.MapContext) c))
        .put(GyroParser.NameContext.class, GET_FIRST_CHILD)
        .put(GyroParser.NumberContext.class, c -> new ValueNode((GyroParser.NumberContext) c))
        .put(GyroParser.ReferenceContext.class, c -> new ReferenceNode((GyroParser.ReferenceContext) c))
        .put(GyroParser.OneAddContext.class, GET_FIRST_CHILD)
        .put(GyroParser.OneAndContext.class, GET_FIRST_CHILD)
        .put(GyroParser.OneMulContext.class, GET_FIRST_CHILD)
        .put(GyroParser.OneMulItemContext.class, GET_FIRST_CHILD)
        .put(GyroParser.OneRelContext.class, GET_FIRST_CHILD)
        .put(GyroParser.OneValueContext.class, GET_FIRST_CHILD)
        .put(GyroParser.PairContext.class, c -> new PairNode((GyroParser.PairContext) c))
        .put(GyroParser.ResourceContext.class, c -> new ResourceNode((GyroParser.ResourceContext) c))
        .put(GyroParser.StatementContext.class, GET_FIRST_CHILD)
        .put(GyroParser.StringContentContext.class, GET_FIRST_CHILD)
        .put(GyroParser.TextContext.class, c -> new ValueNode(c.getText()))
        .put(GyroParser.TwoAddContext.class, c -> new BinaryNode((GyroParser.TwoAddContext) c))
        .put(GyroParser.TwoAndContext.class, c -> new BinaryNode((GyroParser.TwoAndContext) c))
        .put(GyroParser.TwoMulContext.class, c -> new BinaryNode((GyroParser.TwoMulContext) c))
        .put(GyroParser.TwoRelContext.class, c -> new BinaryNode((GyroParser.TwoRelContext) c))
        .put(GyroParser.TwoValueContext.class, c -> new BinaryNode((GyroParser.TwoValueContext) c))
        .put(GyroParser.TypeContext.class, c -> new ValueNode(c.getText()))
        .put(GyroParser.ValueContext.class, GET_FIRST_CHILD)
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
        Function<ParseTree, Node> nodeConstructor = NODE_CONSTRUCTORS.get(contextClass);
        Node node;

        if (nodeConstructor != null) {
            node = nodeConstructor.apply(context);

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

    public static List<Node> create(List<? extends ParseTree> contexts) {
        return contexts.stream()
            .map(Node::create)
            .collect(ImmutableCollectors.toList());
    }

    private static <T extends ParseTree, U extends ParseTree> List<Node> create(T context, Function<T, List<U>> toList) {
        return Optional.ofNullable(context)
            .map(toList)
            .map(Node::create)
            .orElseGet(ImmutableList::of);
    }

    public static List<Node> create(GyroParser.ArgumentsContext context) {
        return create(context, GyroParser.ArgumentsContext::value);
    }

    public static List<Node> create(GyroParser.BodyContext context) {
        return create(context, GyroParser.BodyContext::statement);
    }

    public static Node parse(String text, Function<GyroParser, ? extends ParseTree> function) {
        return parse(CharStreams.fromString(text), function);
    }

    public static Node parse(InputStream input, String file, Function<GyroParser, ? extends ParseTree> function) throws IOException {
        return parse(CharStreams.fromReader(new InputStreamReader(input), file), function);
    }

    private static Node parse(CharStream charStream, Function<GyroParser, ? extends ParseTree> function) {
        GyroLexer lexer = new GyroLexer(charStream);
        CommonTokenStream stream = new CommonTokenStream(lexer);
        GyroParser parser = new GyroParser(stream);
        GyroErrorListener errorListener = new GyroErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        parser.setErrorHandler(new GyroErrorStrategy());

        ParseTree tree = function.apply(parser);
        int errorCount = errorListener.getSyntaxErrors();

        if (errorCount > 0) {
            throw new GyroLanguageException(String.format(
                "%d %s found while parsing.",
                errorCount,
                errorCount == 1 ? "error" : "errors"));
        }

        return Node.create(tree);
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
