package gyro.lang.ast;

import java.util.List;
import java.util.stream.Collectors;

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

public interface NodeVisitor<C, R> {

    default R visit(Node node, C context) {
        return node.accept(this, context);
    }

    default List<R> visit(List<Node> nodes, C context) {
        return nodes.stream()
            .map(n -> visit(n, context))
            .collect(Collectors.toList());
    }

    R visitDirective(DirectiveNode node, C context);

    R visitPair(PairNode node, C context);

    R visitFile(FileNode node, C context);

    R visitKeyBlock(KeyBlockNode node, C context);

    R visitResource(ResourceNode node, C context);

    R visitBinary(BinaryNode node, C context);

    R visitIndexed(IndexedNode node, C context);

    R visitInterpolatedString(InterpolatedStringNode node, C context);

    R visitList(ListNode node, C context);

    R visitMap(MapNode node, C context);

    R visitReference(ReferenceNode node, C context);

    R visitValue(ValueNode node, C context);

}
