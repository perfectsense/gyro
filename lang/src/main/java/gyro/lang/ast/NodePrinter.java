package gyro.lang.ast;

import java.io.IOException;
import java.util.Iterator;
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
import gyro.util.Bug;

public class NodePrinter implements NodeVisitor<PrinterContext, Void, IOException> {

    public static String toString(Node node) {
        StringBuilder builder = new StringBuilder();
        NodePrinter printer = new NodePrinter();
        PrinterContext context = new PrinterContext(builder, 0);

        try {
            printer.visit(node, context);

        } catch (IOException error) {
            throw new Bug(error);
        }

        return builder.toString();
    }

    private void visitBody(List<Node> body, PrinterContext context) throws IOException {
        for (Node item : body) {
            context.appendNewline();
            visit(item, context);
        }
    }

    @Override
    public Void visitDirective(DirectiveNode node, PrinterContext context) throws IOException {
        List<Node> body = node.getBody();
        boolean bodyEmpty = body.isEmpty();

        context.appendNewline();
        context.append('@');
        context.append(node.getName());

        if (bodyEmpty) {
            context.append(':');
        }

        for (Node arg : node.getArguments()) {
            context.append(' ');
            visit(arg, context);
        }

        if (!bodyEmpty) {
            visitBody(body, context.indented());
            context.appendNewline();
            context.append("@end");
        }

        return null;
    }

    @Override
    public Void visitPair(PairNode node, PrinterContext context) throws IOException {
        visit(node.getKey(), context);
        context.append(": ");
        visit(node.getValue(), context.indented());

        return null;
    }

    @Override
    public Void visitFile(FileNode node, PrinterContext context) throws IOException {
        visitBody(node.getBody(), context);

        return null;
    }

    @Override
    public Void visitKeyBlock(KeyBlockNode node, PrinterContext context) throws IOException {
        context.appendNewline();
        context.append(node.getKey());
        visitBody(node.getBody(), context.indented());
        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitResource(ResourceNode node, PrinterContext context) throws IOException {
        context.appendNewline();
        context.append(node.getType());

        Node nameNode = node.getName();

        if (nameNode != null) {
            context.append(' ');
            visit(nameNode, context);
        }

        visitBody(node.getBody(), context.indented());
        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitBinary(BinaryNode node, PrinterContext context) throws IOException {
        visit(node.getLeft(), context);
        context.append(' ');
        context.append(node.getOperator());
        context.append(' ');
        visit(node.getRight(), context);

        return null;
    }

    @Override
    public Void visitIndexed(IndexedNode node, PrinterContext context) throws IOException {
        visit(node.getValue(), context);

        for (Node index : node.getIndexes()) {
            context.append('.');
            visit(index, context);
        }

        return null;
    }

    @Override
    public Void visitInterpolatedString(InterpolatedStringNode node, PrinterContext context) throws IOException {
        context.append('"');

        for (Object item : node.getItems()) {
            if (item instanceof ValueNode) {
                context.append(String.valueOf(((ValueNode) item).getValue()));

            } else {
                visit((Node) item, context);
            }
        }

        context.append('"');

        return null;
    }

    @Override
    public Void visitList(ListNode node, PrinterContext context) throws IOException {
        context.append('[');

        for (Iterator<Node> i = node.getItems().iterator(); i.hasNext(); ) {
            Node item = i.next();

            visit(item, context);

            if (i.hasNext()) {
                context.append(", ");
            }
        }

        context.append(']');

        return null;
    }

    @Override
    public Void visitMap(MapNode node, PrinterContext context) throws IOException {
        context.append('{');

        for (Iterator<PairNode> i = node.getEntries().iterator(); i.hasNext(); ) {
            PairNode entry = i.next();

            visit(entry, context);

            if (i.hasNext()) {
                context.append(", ");
            }
        }

        context.append('}');

        return null;
    }

    @Override
    public Void visitReference(ReferenceNode node, PrinterContext context) throws IOException {
        context.append("$(");

        for (Iterator<Node> i = node.getArguments().iterator(); i.hasNext(); ) {
            visit(i.next(), context);

            if (i.hasNext()) {
                context.append(' ');
            }
        }

        context.append(")");

        return null;
    }

    @Override
    public Void visitValue(ValueNode node, PrinterContext context) throws IOException {
        Object value = node.getValue();

        if (value instanceof String) {
            context.append('\'');
            context.append((String) value);
            context.append('\'');

        } else {
            context.append(value.toString());
        }

        return null;
    }

}
