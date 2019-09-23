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

import com.google.common.base.Preconditions;
import gyro.parser.antlr4.GyroParser;

public class PairNode extends Node {

    private final Node key;
    private final Node value;

    public PairNode(Node key, Node value) {
        super(null);

        this.key = Preconditions.checkNotNull(key);
        this.value = Preconditions.checkNotNull(value);
    }

    public PairNode(GyroParser.PairContext context) {
        super(Preconditions.checkNotNull(context));

        this.key = Node.create(context.key());
        this.value = Node.create(context.value());
    }

    public Node getKey() {
        return key;
    }

    public Node getValue() {
        return value;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitPair(this, context);
    }

}
