/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.lang.ast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.lang.EscapeException;
import gyro.lang.GyroCharStream;
import gyro.lang.GyroErrorListener;
import gyro.lang.GyroErrorStrategy;
import gyro.lang.SyntaxError;
import gyro.lang.SyntaxErrorException;
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
import gyro.util.Bug;
import gyro.util.ImmutableCollectors;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class Node extends Rule {

    private static final Function<ParseTree, Node> GET_FIRST_CHILD = c -> Node.create(c.getChild(0));

    private static final Map<Class<? extends ParseTree>, Function<ParseTree, Node>> NODE_CONSTRUCTORS = ImmutableMap.<Class<? extends ParseTree>, Function<ParseTree, Node>>builder()
        .put(GyroParser.BoolContext.class, c -> new ValueNode((GyroParser.BoolContext) c))
        .put(GyroParser.DirectiveContext.class, c -> new DirectiveNode((GyroParser.DirectiveContext) c))
        .put(GyroParser.FileContext.class, c -> new FileNode((GyroParser.FileContext) c))
        .put(GyroParser.GroupedMulItemContext.class, c -> Node.create(((GyroParser.GroupedMulItemContext) c).value()))
        .put(GyroParser.IndexContext.class, GET_FIRST_CHILD)
        .put(GyroParser.IndexedMulItemContext.class, c -> new IndexedNode((GyroParser.IndexedMulItemContext) c))
        .put(
            GyroParser.InterpolatedStringContext.class,
            c -> new InterpolatedStringNode((GyroParser.InterpolatedStringContext) c))
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
        .put(GyroParser.TextContext.class, c -> new ValueNode((GyroParser.TextContext) c))
        .put(GyroParser.TwoAddContext.class, c -> new BinaryNode((GyroParser.TwoAddContext) c))
        .put(GyroParser.TwoAndContext.class, c -> new BinaryNode((GyroParser.TwoAndContext) c))
        .put(GyroParser.TwoMulContext.class, c -> new BinaryNode((GyroParser.TwoMulContext) c))
        .put(GyroParser.TwoRelContext.class, c -> new BinaryNode((GyroParser.TwoRelContext) c))
        .put(GyroParser.TwoValueContext.class, c -> new BinaryNode((GyroParser.TwoValueContext) c))
        .put(GyroParser.TypeContext.class, c -> new ValueNode((GyroParser.TypeContext) c))
        .put(GyroParser.ValueContext.class, GET_FIRST_CHILD)
        .put(GyroParser.WordContext.class, c -> new ValueNode((GyroParser.WordContext) c))
        .build();

    public static Node create(ParseTree context) {
        Class<? extends ParseTree> contextClass = context.getClass();
        Function<ParseTree, Node> nodeConstructor = NODE_CONSTRUCTORS.get(contextClass);
        Node node;

        if (nodeConstructor != null) {
            node = nodeConstructor.apply(context);

        } else if (TerminalNode.class.isAssignableFrom(contextClass)) {
            node = new ValueNode((TerminalNode) context);

        } else {
            throw new Bug(String.format(
                "@|bold %s|@ isn't a known node type!",
                contextClass.getName()));
        }

        return node;
    }

    public static List<Node> create(List<? extends ParseTree> contexts) {
        return contexts.stream()
            .map(Node::create)
            .collect(ImmutableCollectors.toList());
    }

    private static <T extends ParseTree, U extends ParseTree> List<Node> create(
        T context,
        Function<T, List<U>> toList) {
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
        return parse(new GyroCharStream(text), function);
    }

    public static Node parse(InputStream input, String file, Function<GyroParser, ? extends ParseTree> function)
        throws IOException {
        return parse(new GyroCharStream(input, file), function);
    }

    private static Node parse(GyroCharStream charStream, Function<GyroParser, ? extends ParseTree> function) {
        GyroErrorListener errorListener = new GyroErrorListener(charStream);
        GyroLexer lexer = new GyroLexer(charStream);

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        CommonTokenStream stream = new CommonTokenStream(lexer);
        GyroParser parser = new GyroParser(stream);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        parser.setErrorHandler(GyroErrorStrategy.INSTANCE);

        ParseTree tree = function.apply(parser);
        List<SyntaxError> errors = errorListener.getSyntaxErrors();

        if (!errors.isEmpty()) {
            throw new SyntaxErrorException(charStream.getSourceName(), errors);
        }

        try {
            return Node.create(tree);

        } catch (EscapeException e) {
            throw new SyntaxErrorException(
                charStream.getSourceName(),
                Collections.singletonList(new SyntaxError(charStream, e.getMessage(), e.getToken())));
        }
    }

    public Node(Token start, Token stop) {
        super(start, stop);
    }

    public Node(ParserRuleContext context) {
        super(context);
    }

    public abstract <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X;

    @Override
    public String toString() {
        return NodePrinter.toString(this);
    }

}
