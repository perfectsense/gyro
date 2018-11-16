package beam.aws.resources;

import beam.aws.AwsCredentials;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Throwables;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public abstract class TaggableResource<T> extends AwsResource implements Taggable {

    private static final String NAME_KEY = "Name";

    private Map<String, String> tags;

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    public String getName() {
        return getTags().get(NAME_KEY);
    }

    public void setName(String name) {
        if (name != null) {
            getTags().put(NAME_KEY, name);

        } else {
            getTags().remove(NAME_KEY);
        }
    }

    protected abstract String getId();

    protected void doInit(T resource) {
    }

    public final void init(T resource) {
        doInit(resource);

        try {
            for (Object item : ObjectUtils.to(Iterable.class, resource)) {
                Method getTagsMethod = item.getClass().getMethod("getTags");

                for (Object tag : ObjectUtils.to(Iterable.class, getTagsMethod.invoke(item))) {
                    Class<?> tagClass = tag.getClass();
                    Method getKeyMethod = tagClass.getMethod("getKey");
                    Method getValueMethod = tagClass.getMethod("getValue");
                    String key = ObjectUtils.to(String.class, getKeyMethod.invoke(tag));

                    if (!key.startsWith("aws:")) {
                        getTags().put(key, ObjectUtils.to(String.class, getValueMethod.invoke(tag)));
                    }
                }
            }

        } catch (IllegalAccessException |
                NoSuchMethodException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error.getCause());
        }
    }

    protected abstract void doCreate();

    @Override
    public final void create() {
        doCreate();
        createTags();
    }

    protected abstract void doUpdate(AwsResource config, Set<String> changedProperties);

    @Override
    public final void update(BeamResource current, Set<String> changedProperties) {
        doUpdate((AwsResource) current, changedProperties);
        createTags();
    }

    private void createTags() {
        Map<String, String> tags = getTags();

        if (tags != null && !tags.isEmpty()) {
            AmazonEC2Client client = createClient(AmazonEC2Client.class);
            CreateTagsRequest ctRequest = new CreateTagsRequest();

            for (Map.Entry<String, String> entry : tags.entrySet()) {
                ctRequest.getTags().add(new Tag(entry.getKey(), entry.getValue()));
            }

            ctRequest.withResources(getId());

            executeService(() -> {
                client.createTags(ctRequest);
                return null;
            });
        }
    }
}
