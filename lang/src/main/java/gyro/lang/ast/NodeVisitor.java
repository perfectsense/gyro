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

import java.util.ArrayList;
import java.util.List;

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

public interface NodeVisitor<C, R, X extends Throwable> {

    default R visit(Node node, C context) throws X {
        return node.accept(this, context);
    }

    default List<R> visit(List<Node> nodes, C context) throws X {
        List<R> list = new ArrayList<>();

        for (Node item : nodes) {
            visit(item, context);
        }

        return list;
    }

    R visitDirective(DirectiveNode node, C context) throws X;

    R visitPair(PairNode node, C context) throws X;

    R visitFile(FileNode node, C context) throws X;

    R visitKeyBlock(KeyBlockNode node, C context) throws X;

    R visitResource(ResourceNode node, C context) throws X;

    R visitBinary(BinaryNode node, C context) throws X;

    R visitIndexed(IndexedNode node, C context) throws X;

    R visitInterpolatedString(InterpolatedStringNode node, C context) throws X;

    R visitList(ListNode node, C context) throws X;

    R visitMap(MapNode node, C context) throws X;

    R visitReference(ReferenceNode node, C context) throws X;

    R visitValue(ValueNode node, C context) throws X;

}
