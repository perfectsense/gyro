package gyro.core.validation;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;

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
            List<String> fieldValidationMessages = validateFields(field, diffable, indent);
            if (!fieldValidationMessages.isEmpty()) {
                validationMessages.addAll(fieldValidationMessages);
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

    private static List<String> validateFields(DiffableField field, Diffable diffable, String indent) {
        List<String> validationMessages = field.validate(diffable);

        return validationMessages.stream().filter(o -> !ObjectUtils.isBlank(o)).map(o -> String.format("%s· %s: %s. %s", indent,
            field.getName(), field.getValue(diffable), o)).collect(Collectors.toList());
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
                        errorList = validateResource(diffableObject, field.getName(), indent + "    ");
                    }
                }
            } else {
                if (object instanceof Diffable) {
                    errorList = validateResource((Diffable) object, field.getName(), indent + "    ");
                }
            }

            if (!errorList.isEmpty()) {
                validationMessages.addAll(errorList);
            }
        }

        return validationMessages;
    }
}
