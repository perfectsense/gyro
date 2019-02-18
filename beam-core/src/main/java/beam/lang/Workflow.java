package beam.lang;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import beam.core.BeamUI;
import beam.core.diff.DiffableField;
import beam.lang.ast.Node;
import beam.lang.ast.block.KeyBlockNode;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.scope.State;
import com.google.common.collect.ImmutableSet;

public class Workflow {

    private final String name;
    private final String triggerType;
    private final Set<String> triggerFields;
    private final String firstStage;
    private final Map<String, Stage> stages = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public Workflow(Scope parent, KeyBlockNode node) throws Exception {
        Scope scope = new Scope(parent);

        for (Iterator<Node> i = node.getBody().iterator(); i.hasNext();) {
            Node item = i.next();

            if (item instanceof KeyBlockNode) {
                KeyBlockNode kb = (KeyBlockNode) item;

                if (kb.getKey().equals("stage")) {
                    Stage stage = new Stage(scope, kb.getBody());

                    stages.put(stage.getName(), stage);
                    i.remove();
                    continue;
                }
            }

            item.evaluate(scope);
        }

        name = (String) scope.get("name");
        triggerType = (String) scope.get("trigger-type");
        triggerFields = ImmutableSet.copyOf((List<String>) scope.get("trigger-fields"));
        firstStage = (String) scope.get("first-stage");
    }

    public String getName() {
        return name;
    }

    public boolean isTriggerable(Resource resource, Set<DiffableField> changedFields) {
        return resource.resourceType().equals(triggerType)
                && changedFields.stream()
                        .map(DiffableField::getBeamName)
                        .anyMatch(triggerFields::contains);
    }

    public void execute(BeamUI ui, State state, Map<String, Object> pendingValues) throws Exception {
        AtomicReference<String> stageName = new AtomicReference<>(firstStage);
        int index = 0;

        do {
            Stage stage = stages.get(stageName.get());

            if (stage == null) {
                throw new IllegalArgumentException(String.format("No stage named [%s]!", stageName));
            }

            ui.write("\n@|magenta %d Executing %s stage|@\n", ++index, stage.getName());

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            ui.indented(() -> {
                stageName.set(stage.execute(ui, state, pendingValues));
            });

        } while (stageName.get() != null);
    }

}
