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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class BlockNode extends Node {

    private final List<Node> body;

    public BlockNode(ParserRuleContext context, List<Node> body) {
        super(context);

        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public List<Node> getBody() {
        return body;
    }
}
