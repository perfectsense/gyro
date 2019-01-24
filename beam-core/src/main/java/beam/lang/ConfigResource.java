package beam.lang;

import java.util.Set;

public class ConfigResource extends Resource {

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        return null;
    }

    @Override
    public Class resourceCredentialsClass() {
        return null;
    }

    @Override
    public void execute() {
    }

}
