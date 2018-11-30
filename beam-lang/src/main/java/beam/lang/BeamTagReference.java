package beam.lang;

import java.util.*;

public class BeamTagReference implements BeamValue {

    private String tag;

    private List<BeamConfig> value;

    public BeamTagReference() {
    }

    public BeamTagReference(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('#');
        sb.append('(');
        sb.append(getTag());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public List<BeamConfig> getValue() {
        return value;
    }

    @Override
    public boolean resolve(BeamConfig config) {
        Map<BeamConfigKey, BeamConfig> tagMap = config.getTaggedConfigs().get(getTag());
        if (tagMap == null) {
            return false;
        }

        value = new ArrayList<>();
        value.addAll(tagMap.values());
        return false;
    }

    @Override
    public Set<BeamConfig> getDependencies(BeamConfig config) {
        return new HashSet<>();
    }
}
