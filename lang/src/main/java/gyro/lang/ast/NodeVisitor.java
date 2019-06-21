package gyro.lang.ast;

import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.lang.ast.condition.AndConditionNode;
import gyro.lang.ast.condition.ComparisonConditionNode;
import gyro.lang.ast.condition.OrConditionNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.control.IfNode;
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

    R visitDirective(DirectiveNode node, C context);

    R visitPair(PairNode node, C context);

    R visitFile(FileNode node, C context);

    R visitKeyBlock(KeyBlockNode node, C context);

    R visitResource(ResourceNode node, C context);

    R visitVirtualResource(VirtualResourceNode node, C context);

    R visitAndCondition(AndConditionNode node, C context);

    R visitComparisonCondition(ComparisonConditionNode node, C context);

    R visitOrCondition(OrConditionNode node, C context);

    R visitValueCondition(ValueConditionNode node, C context);

    R visitFor(ForNode node, C context);

    R visitIf(IfNode node, C context);

    R visitIndexedNode(IndexedNode node, C context);

    R visitInterpolatedString(InterpolatedStringNode node, C context);

    R visitList(ListNode node, C context);

    R visitMap(MapNode node, C context);

    R visitReference(ReferenceNode node, C context);

    R visitValue(ValueNode node, C context);

}
