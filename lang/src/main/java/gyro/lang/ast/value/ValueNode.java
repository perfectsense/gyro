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

package gyro.lang.ast.value;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import gyro.lang.EscapeException;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ValueNode extends Node {

    private final Object value;

    // Escape character
    private static final Map<Character, Character> ESCAPE = new ImmutableMap.Builder<Character, Character>()
        .put('t', '\t')
        .put('n', '\n')
        .put('r', '\r')
        .put('"', '"')
        .put('\'', '\'')
        .put('\\', '\\').build();

    public ValueNode(Object value) {
        super(null);

        this.value = Preconditions.checkNotNull(value);
    }

    public ValueNode(GyroParser.BoolContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = context.TRUE() != null;
    }

    public ValueNode(GyroParser.LiteralStringContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = getContextText(context.stringLiteral());
    }

    public ValueNode(GyroParser.NumberContext context) {
        super(Preconditions.checkNotNull(context));

        String text = context.getText();

        if (text.contains(".")) {
            this.value = Double.valueOf(text);

        } else {
            this.value = Long.valueOf(text);
        }
    }

    public ValueNode(GyroParser.TextContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = getContextText(context);
    }

    public ValueNode(GyroParser.TypeContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = context.getText();
    }

    public ValueNode(GyroParser.WordContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = context.getText();
    }

    public ValueNode(TerminalNode context) {
        super(context.getSymbol(), context.getSymbol());

        this.value = context.getText();
    }

    public Object getValue() {
        return value;
    }

    private String getContextText(RuleContext context) {
        if (context.getChildCount() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < context.getChildCount(); i++) {
            ParseTree child = context.getChild(i);

            if (child instanceof GyroParser.EscapeContext) {
                builder.append(getEscapeText((GyroParser.EscapeContext) child));

            } else {
                builder.append(child.getText());
            }
        }

        return builder.toString();
    }

    private String getEscapeText(GyroParser.EscapeContext context) {
        return ESCAPE.entrySet()
            .stream()
            .filter(e -> e.getKey().equals(context.getText().charAt(1)))
            .findFirst()
            .map(Map.Entry::getValue)
            .map(String::valueOf)
            .orElseThrow(() -> new EscapeException(context.getText(), context.ESCAPE().getSymbol()));
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitValue(this, context);
    }

}
