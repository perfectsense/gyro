package beam.aws;

import com.amazonaws.services.ec2.model.Tag;

public class AWSUtils {

    /**
     * Returns the tag value associated with the given {@code key} from the
     * given {@code tags}.
     *
     * @param tags Can't be {@code null}.
     * @param key May be {@code null}.
     * @return May be {@code null}.
     */
    public static String getTagValue(Iterable<Tag> tags, String key) {
        for (Tag tag : tags) {
            if (tag.getKey().equals(key)) {
                return tag.getValue();
            }
        }

        return null;
    }
}
