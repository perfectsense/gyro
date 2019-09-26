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

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ValueNode extends Node {

    private final Object value;

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

        this.value = context.stringLiteral().getText();
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

        this.value = context.getText();
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

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitValue(this, context);
    }

}
