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

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class IndexedNode extends Node {

    private final Node value;
    private final List<Node> indexes;

    public IndexedNode(Node value, List<Node> indexes) {
        super(null);

        this.value = Preconditions.checkNotNull(value);
        this.indexes = ImmutableList.copyOf(Preconditions.checkNotNull(indexes));
    }

    public IndexedNode(GyroParser.IndexedMulItemContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = Node.create(context.item());
        this.indexes = Node.create(context.index());
    }

    public Node getValue() {
        return value;
    }

    public List<Node> getIndexes() {
        return indexes;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitIndexed(this, context);
    }

}
