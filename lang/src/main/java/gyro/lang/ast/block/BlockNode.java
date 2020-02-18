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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
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

    public static String validateLocalImmutability(BlockNode blockNode) {
        return validateLocalImmutability(PairNode.getNodeVariables(blockNode.getBody()));
    }

    public static String validateLocalImmutability(List<String> nodeVariables) {
        return nodeVariables.stream()
            .filter(e -> Collections.frequency(nodeVariables, e) > 1)
            .findFirst().orElse(null);
    }

    public static String validateGlobalImmutability(BlockNode blockNode, List<Node> globalNode) {
        List<String> nodeVariables = PairNode.getNodeVariables(blockNode.getBody());

        Set<String> globalKeys = new HashSet<>(PairNode.getNodeVariables(globalNode));

        return nodeVariables.stream()
            .filter(globalKeys::contains)
            .findFirst().orElse(null);
    }

}
