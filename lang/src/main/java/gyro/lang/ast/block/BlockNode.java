package gyro.lang.ast.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

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
