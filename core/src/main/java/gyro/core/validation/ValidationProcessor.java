package gyro.core.validation;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationProcessor {
    public static List<String> validationMessages(Diffable diffable) {
        return validateResource(diffable, diffable.primaryKey(), "");
    }

    private static List<String> validateResource(Diffable diffable, String resourceName, String indent) {
        List<DiffableField> fields = DiffableType.getInstance(diffable.getClass()).getFields();

        List<String> validationMessages = new ArrayList<>();

        for (DiffableField field : fields) {
            String validationMessage = validateFields(field, diffable, indent);
            if (!ObjectUtils.isBlank(validationMessage)) {
                validationMessages.add(validationMessage);
            }
        }

        List<String> customValidations = diffable.validations();

        validationMessages.addAll(customValidations.stream().map(message -> String.format("%s· %s", indent, message)).collect(Collectors.toList()));

        for (DiffableField field : fields) {
            List<String> validateComplexFieldMessages = validateComplexFields(field, diffable, indent);
            validationMessages.addAll(validateComplexFieldMessages);
        }

        if (!validationMessages.isEmpty()) {
            validationMessages.add(0,String.format("\n%sx %s", indent, resourceName));
        }

        return validationMessages;
    }

    private static String validateFields(DiffableField field, Diffable diffable, String indent) {
        String validationMessage = field.validate(diffable);

        if (!ObjectUtils.isBlank(validationMessage)) {
            validationMessage = String.format("%s· %s: %s. %s", indent,
                field.getGyroName(), field.getValue(diffable), validationMessage);
        }

        return validationMessage;
    }

    private static List<String> validateComplexFields(DiffableField field, Diffable diffable, String indent) {
        List<String> validationMessages = new ArrayList<>();

        Object object = field.getValue(diffable);

        if (object != null) {
            List<String> errorList = new ArrayList<>();
            if (object instanceof List) {
                List invokeList = (List) object;
                if (!invokeList.isEmpty() && invokeList.get(0) instanceof Diffable) {
                    for (Object invokeListObject : invokeList) {
                        Diffable diffableObject = (Diffable) invokeListObject;
                        errorList = validateResource(diffableObject, field.getGyroName(), indent + "    ");
                    }
                }
            } else {
                if (object instanceof Diffable) {
                    errorList = validateResource((Diffable) object, field.getGyroName(), indent + "    ");
                }
            }

            if (!errorList.isEmpty()) {
                validationMessages.addAll(errorList);
            }
        }

        return validationMessages;
    }
}
