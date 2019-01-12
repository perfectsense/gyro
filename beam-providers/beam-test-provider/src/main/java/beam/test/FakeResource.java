package beam.test;

import beam.lang.Resource;

import java.util.Set;

public abstract class FakeResource extends Resource {

    @Override
    public boolean refresh() {
        return true;
    }

    @Override
    public void create() {

    }

    @Override
    public void update(Resource resource, Set<String> set) {

    }

    @Override
    public void delete() {

    }

    @Override
    public Class resourceCredentialsClass() {
        return TestCredentials.class;
    }

}
