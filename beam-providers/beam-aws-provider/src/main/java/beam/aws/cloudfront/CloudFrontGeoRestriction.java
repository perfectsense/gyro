package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;
import software.amazon.awssdk.services.cloudfront.model.GeoRestriction;
import software.amazon.awssdk.services.cloudfront.model.Restrictions;

import java.util.ArrayList;
import java.util.List;

public class CloudFrontGeoRestriction extends Diffable {

    private String type;
    private List<String> restrictions;

    public CloudFrontGeoRestriction() {
    }

    public CloudFrontGeoRestriction(GeoRestriction geoRestriction) {
        setType(geoRestriction.restrictionTypeAsString());
        setRestrictions(geoRestriction.items());
    }

    @ResourceDiffProperty(updatable = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getRestrictions() {
        if (restrictions == null) {
            restrictions = new ArrayList<>();
        }

        return restrictions;
    }

    public void setRestrictions(List<String> restrictions) {
        this.restrictions = restrictions;
    }

    public Restrictions toRestrictions() {
        return Restrictions.builder()
            .geoRestriction(r -> r.restrictionType(getType())
                .items(getRestrictions())
                .quantity(getRestrictions().size())).build();
    }

    @Override
    public String primaryKey() {
        return "geo-restriction";
    }

    @Override
    public String toDisplayString() {
        return "geo restriction";
    }
}
