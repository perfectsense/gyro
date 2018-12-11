package beam.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForExtension extends BeamExtension {

    private boolean expanded;

    @Override
    public String getType() {
        return "for";
    }

    @Override
    protected boolean resolve(BeamContext parent, BeamContext root) {
        List<BeamResolvable> variables = new ArrayList<>();
        List<BeamResolvable> values = new ArrayList<>();
        List<BeamResolvable> current = variables;
        for (BeamResolvable resolvable : getParams()) {
            if ("in".equals(resolvable.getValue())) {
                current = values;
            } else {
                current.add(resolvable);
            }
        }

        if (variables.isEmpty() || values.isEmpty()) {
            throw new BeamLangException("variables or value list cannot be empty");
        }

        if (variables.size() != values.size()) {
            throw new BeamLangException(String.format("The number of variables (%s) does not match the number of value list (%s)", variables.size(), values.size()));
        }

        Integer size = null;
        for (BeamResolvable list : values) {
            BeamList beamList = (BeamList) list;
            if (size == null) {
                size = beamList.getList().size();
            } else if (size != beamList.getList().size()) {
                throw new BeamLangException(String.format("The sizes of value list does not match (%s vs %s)", size, beamList.getList().size()));
            }
        }

        Map<BeamContextKey, BeamReferable> oldReferables = new HashMap<>();
        for (BeamResolvable resolvable : variables) {
            String varId = resolvable.getValue().toString();
            BeamContextKey key = new BeamContextKey(varId);
            if (root.hasKey(key)) {
                oldReferables.put(key, root.getReferable(key));
            }
        }

        boolean progress = false;
        if (!expanded) {
            List<BeamConfig> newList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                for (BeamConfig unResolvedConfig : getSubConfigs()) {
                    // need to find extensions based on type
                    ConfigParser parser = new ConfigParser();

                    BeamConfig clone = parser.parse(unResolvedConfig.getCtx());
                    newList.add(clone);
                }
            }

            setSubConfigs(newList);
            expanded = true;
        }

        int index = 0;
        int originSize = getSubConfigs().size() / size;
        for (BeamConfig unResolvedConfig : getSubConfigs()) {
            int valueIndex = index++ / originSize;
            for (int i = 0; i < variables.size(); i++) {
                String varId = variables.get(i).getValue().toString();
                BeamList valueList = (BeamList) values.get(i);
                root.addReferable(new BeamContextKey(varId), valueList.getList().get(valueIndex));
            }

            if (unResolvedConfig.resolveParams(root)) {
                progress = unResolvedConfig.resolve(parent, root) || progress;
            }
        }

        for (BeamResolvable resolvable : variables) {
            String varId = resolvable.getValue().toString();
            BeamContextKey key = new BeamContextKey(varId);
            if (oldReferables.containsKey(key)) {
                root.addReferable(key, oldReferables.get(key));
            } else {
                root.removeReferable(key);
            }
        }

        return progress;
    }
}
