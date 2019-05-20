package gyro.core.finder;

import java.util.HashMap;
import java.util.Map;

import gyro.core.resource.Settings;

public class FinderSettings extends Settings {

    private Map<String, Class<? extends Finder>> finderClasses;

    public Map<String, Class<? extends Finder>> getFinderClasses() {
        if (finderClasses == null) {
            finderClasses = new HashMap<>();
        }

        return finderClasses;
    }

    public void setFinderClasses(Map<String, Class<? extends Finder>> finderClasses) {
        this.finderClasses = finderClasses;
    }

}
