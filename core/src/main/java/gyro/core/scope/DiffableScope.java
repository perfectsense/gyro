package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import gyro.core.GyroException;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableProcessor;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.BlockNode;

public class DiffableScope extends Scope {

    private final BlockNode block;
    private final List<DiffableProcessor> processors = new ArrayList<>();
    private List<Node> stateNodes = new ArrayList<>();

    public DiffableScope(Scope parent, BlockNode block) {
        super(parent);
        this.block = block;
    }

    public BlockNode getBlock() {
        if (block != null) {
            return block;

        } else {
            return Optional.ofNullable(getParent())
                .map(p -> p.getClosest(DiffableScope.class))
                .map(DiffableScope::getBlock)
                .orElse(null);
        }
    }

    public List<Node> getStateNodes() {
        return stateNodes;
    }

    public void setStateNodes(List<Node> stateNodes) {
        this.stateNodes = stateNodes;
    }

    public void addProcessor(DiffableProcessor processor) {
        processors.add(processor);
    }

    public void process(Diffable diffable) {
        Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);

        for (DiffableProcessor processor : processors) {
            try {
                Set<String> fields = processor.process(diffable);

                if (fields != null) {
                    configuredFields.addAll(fields);
                }

            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't process @|bold %s|@ using @|bold %s|@!", diffable, processor),
                    error);
            }
        }
    }

}
