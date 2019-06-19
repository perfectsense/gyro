package gyro.core.scope;

import java.lang.reflect.Field;
import java.util.Set;

import gyro.core.Namespace;
import gyro.core.Type;
import gyro.core.resource.Diffable;
import gyro.core.resource.Resource;

@Namespace("test")
@Type("resource")
public class TestResource extends Resource {

    private static final Field nameField;

    static {
        try {
            nameField = Diffable.class.getDeclaredField("name");
            nameField.setAccessible(true);

        } catch (NoSuchFieldException error) {
            throw new IllegalStateException(error);
        }
    }

    public TestResource(String name) {
        try {
            nameField.set(this, name);

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
    }

    @Override
    public void update(Resource current, Set<String> changedFieldNames) {
    }

    @Override
    public void delete() {
    }

    @Override
    public String toDisplayString() {
        return null;
    }

}
