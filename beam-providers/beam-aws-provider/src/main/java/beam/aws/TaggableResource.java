package beam.aws;

import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import com.psddev.dari.util.CompactMap;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.paginators.DescribeTagsIterable;
import java.util.ArrayList;
import java.util.List;
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

        Ec2Client client = createClient(Ec2Client.class);
        DescribeTagsIterable response = client.describeTagsPaginator(
                r -> r.filters(
                f -> f.name("resource-id")
                        .values(getId())
                        .build())
                .build());

        response.stream().forEach(
                r -> r.tags().forEach(
                        t -> getTags().put(t.key(), t.value())));

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
        Ec2Client client = createClient(Ec2Client.class);

        Map<String, String> tags = getTags();
        if (tags != null && !tags.isEmpty()) {

            List<Tag> tagObjects = new ArrayList<>();
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                tagObjects.add(Tag.builder().key(entry.getKey()).value(entry.getValue()).build());
            }

            CreateTagsRequest request = CreateTagsRequest.builder()
                    .resources(getId())
                    .tags(tagObjects)
                    .build();

            executeService(() -> {
                client.createTags(request);
                return null;
            });
        }
    }
}
