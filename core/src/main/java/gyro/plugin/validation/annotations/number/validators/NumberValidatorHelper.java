package gyro.plugin.validation.annotations.number.validators;

import java.util.Arrays;

public class NumberValidatorHelper {
    static boolean minMaxValidator(Object value, Object ref, Class<?> type, boolean isMax) {
        if (value == null) {
            return false;
        }

        int result = 0;

        if (type.equals(Integer.class)) {
            Integer valueCheck = (Integer) value;

            Integer minDigitsValue = (Integer) ref;

            result = Integer.compare(valueCheck, minDigitsValue);
        } else if (type.equals(Double.class)) {
            Double valueCheck = (Double) value;

            Double minDigitsValue = (Double) ref;

            result = Double.compare(valueCheck, minDigitsValue);
        } else if (type.equals(Long.class)) {
            Double valueCheck = (Double) value;

            Double minDigitsValue = (Double) ref;

            result = Double.compare(valueCheck, minDigitsValue);
        }

        if (0 == result) {
            return isMax;
        } else if (result < 0) {
            return isMax;
        } else {
            return !isMax;
        }
    }

    static boolean listValidator(Object value, Object ref, Class<?> type) {
        if (value == null) {
            return false;
        }

        if (type.equals(Integer.class)) {
            Integer valueCheck = (Integer) value;
            int[] refList = (int[]) ref;
            return Arrays.stream(refList).anyMatch(o -> o == valueCheck);
        } else if (type.equals(Double.class)) {
            Double valueCheck = (Double) value;
            double[] refList = (double[]) ref;
            return Arrays.stream(refList).anyMatch(o -> o == valueCheck);
        } else if (type.equals(Long.class)) {
            Long valueCheck = (Long) value;
            long[] refList = (long[]) ref;
            return Arrays.stream(refList).anyMatch(o -> o == valueCheck);
        }

        return false;
    }

    static boolean rangeValidator(Object value, Object refLow, Object refHigh, Class<?> type) {
        if (value == null) {
            return false;
        }

        if (type.equals(Integer.class)) {
            Integer valueCheck = (Integer) value;
            Integer low = (Integer) refLow;
            Integer high = (Integer) refHigh;
            return valueCheck >= low && valueCheck <= high;
        } else if (type.equals(Double.class)) {
            Double valueCheck = (Double) value;
            Double low = (Double) refLow;
            Double high = (Double) refHigh;
            return valueCheck >= low && valueCheck <= high;
        } else if (type.equals(Long.class)) {
            Long valueCheck = (Long) value;
            Long low = (Long) refLow;
            Long high = (Long) refHigh;
            return valueCheck >= low && valueCheck <= high;
        }

        return false;
    }
}
