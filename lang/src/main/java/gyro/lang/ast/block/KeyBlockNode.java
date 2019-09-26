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

package gyro.lang.ast.block;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class KeyBlockNode extends BlockNode {

    private final String key;
    private final Node name;

    public KeyBlockNode(String key, Node name, List<Node> body) {
        super(null, body);

        this.key = Preconditions.checkNotNull(key);
        this.name = name;
    }

    public KeyBlockNode(GyroParser.KeyBlockContext context) {
        super(Preconditions.checkNotNull(context), Node.create(context.body()));

        this.key = context.IDENTIFIER().getText();
        this.name = Optional.ofNullable(context.name()).map(Node::create).orElse(null);
    }

    public String getKey() {
        return key;
    }

    public Node getName() {
        return name;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitKeyBlock(this, context);
    }

}
