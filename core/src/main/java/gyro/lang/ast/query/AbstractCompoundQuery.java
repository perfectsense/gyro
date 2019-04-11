package gyro.lang.ast.query;

import com.google.common.collect.ImmutableList;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public abstract class AbstractCompoundQuery extends Query {

    private final List<Query> children;

    public AbstractCompoundQuery(GyroParser.QueryContext context) {
        ImmutableList.Builder<Query> list = ImmutableList.builder();
        addChildren(list, Query.create(context.getChild(0)));
        addChildren(list, Query.create(context.getChild(2)));
        children = list.build();
    }

    public AbstractCompoundQuery(List<Query> children) {
        this.children = ImmutableList.copyOf(children);
    }

    public List<Query> getChildren() {
        return children;
    }

    private void addChildren(ImmutableList.Builder<Query> list, Query child) {
        if (getClass().isInstance(child)) {
            list.addAll(((AbstractCompoundQuery) child).getChildren());
        } else {
            list.add(child);
        }
    }
}
