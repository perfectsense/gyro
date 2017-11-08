package beam.aws.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Throwables;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

public abstract class TaggableEC2Resource<R> extends AWSResource<R> implements Taggable {

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

    public Set<String> taggableAwsIds() {
        return Collections.singleton(awsId());
    }

    protected void doInit(AWSCloud cloud, BeamResourceFilter filter, R resource) {
    }

    @Override
    public final void init(AWSCloud cloud, BeamResourceFilter filter, R resource) {
        doInit(cloud, filter, resource);

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

    protected abstract void doCreate(AWSCloud cloud);

    @Override
    public final void create(AWSCloud cloud) {
        doCreate(cloud);
        createTags(cloud);
    }

    protected abstract void doUpdate(AWSCloud cloud, AWSResource<R> config, Set<String> changedProperties);

    @Override
    public final void update(AWSCloud cloud, BeamResource<AWSCloud, R> current, Set<String> changedProperties) {
        doUpdate(cloud, (AWSResource<R>) current, changedProperties);
        createTags(cloud);
    }

    private void createTags(AWSCloud cloud) {
        Set<String> awsIds = taggableAwsIds();

        if (awsIds != null && !awsIds.isEmpty()) {
            Map<String, String> tags = getTags();

            if (tags != null && !tags.isEmpty()) {
                AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
                CreateTagsRequest ctRequest = new CreateTagsRequest();

                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    ctRequest.getTags().add(new Tag(entry.getKey(), entry.getValue()));
                }

                ctRequest.setResources(awsIds);

                executeService(() -> {
                    client.createTags(ctRequest);
                    return null;
                });
            }
        }
    }
}
