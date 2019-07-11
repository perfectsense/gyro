package gyro.lang.filter;

import com.google.common.collect.ImmutableList;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public abstract class AbstractCompoundFilter extends Filter {

    private final List<Filter> children;

    public AbstractCompoundFilter(GyroParser.FilterContext context) {
        ImmutableList.Builder<Filter> list = ImmutableList.builder();
        addChildren(list, create(context.getChild(0)));
        addChildren(list, create(context.getChild(2)));
        children = list.build();
    }

    public AbstractCompoundFilter(List<Filter> children) {
        this.children = ImmutableList.copyOf(children);
    }

    public List<Filter> getChildren() {
        return children;
    }

    private void addChildren(ImmutableList.Builder<Filter> list, Filter child) {
        if (getClass().isInstance(child)) {
            list.addAll(((AbstractCompoundFilter) child).getChildren());
        } else {
            list.add(child);
        }
    }
}
