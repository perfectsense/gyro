package beam.config;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beam.Beam;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.util.ObjectUtils;

public class ConfigConstructor extends Constructor {

    private static final LoadingCache<Class<?>, String> CONFIG_KEYS = CacheBuilder.
            newBuilder().
            build(new CacheLoader<Class<?>, String>() {

                @Override
                public String load(Class<?> superClass) {
                    ConfigKey configKey = superClass.getAnnotation(ConfigKey.class);

                    return configKey != null ? configKey.value() : "";
                }
            });

    private static final LoadingCache<Class<?>, Map<String, Class<?>>> CONFIG_VALUES = CacheBuilder.
            newBuilder().
            build(new CacheLoader<Class<?>, Map<String, Class<?>>>() {

                @Override
                public Map<String, Class<?>> load(Class<?> superClass) {
                    Map<String, Class<?>> map = new HashMap<String, Class<?>>();

                    for (Class<?> subClass : Beam.reflections.getSubTypesOf(superClass)) {
                        ConfigValue configValue = subClass.getAnnotation(ConfigValue.class);

                        if (configValue != null) {
                            map.put(configValue.value(), subClass);
                        }
                    }

                    return map;
                }
            });

    private final File configFile;

    public ConfigConstructor(Class<? extends Object> theRoot, File configFile) {
        super(theRoot);

        this.configFile = configFile;
    }

    @Override
    protected Construct getConstructor(Node node) {
        if (node instanceof ScalarNode &&
                File.class.isAssignableFrom(node.getType())) {

            return new AbstractConstruct() {

                @Override
                public Object construct(Node node) {
                    String parent = configFile.getParent();

                    return new File(
                            parent == null ? "." : parent,
                            ((ScalarNode) node).getValue());
                }
            };

        } else if (node instanceof MappingNode) {
            List<NodeTuple> children = ((MappingNode) node).getValue();

            if (children != null && !children.isEmpty()) {
                String configKey = CONFIG_KEYS.getUnchecked(node.getType());

                if (!ObjectUtils.isBlank(configKey)) {
                    boolean foundValueNode = false;
                    for (NodeTuple child : children) {
                        Node keyNode = child.getKeyNode();

                        if (keyNode instanceof ScalarNode &&
                                ((ScalarNode) keyNode).getValue().equals(configKey)) {
                            Node valueNode = child.getValueNode();

                            if (valueNode instanceof ScalarNode) {
                                Class<?> c = CONFIG_VALUES.getUnchecked(node.getType()).get(((ScalarNode) valueNode).getValue());

                                if (c != null) {
                                    node.setType(c);
                                    foundValueNode = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!foundValueNode) {
                        Class<?> c = CONFIG_VALUES.getUnchecked(node.getType()).get("default");

                        if (c != null) {
                            node.setType(c);
                        }
                    }
                }
            }
        }

        return super.getConstructor(node);
    }
}
