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

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class FileNode extends BlockNode {

    public FileNode(GyroParser.FileContext context) {
        super(Preconditions.checkNotNull(context), Node.create(context.statement()));
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitFile(this, context);
    }

}
