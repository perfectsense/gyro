package gyro.plugin.validation;

import com.google.common.base.CaseFormat;
import gyro.core.BeamException;
import gyro.core.diff.Diffable;
import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.AnnotationProcessorClass;
import gyro.plugin.validation.annotations.GyroValidation;
import gyro.plugin.validation.annotations.misc.validators.RequiredValidator;
import gyro.plugin.validation.annotations.number.validators.ListDoubleValidator;
import gyro.plugin.validation.annotations.number.validators.ListIntegerValidator;
import gyro.plugin.validation.annotations.number.validators.ListLongValidator;
import gyro.plugin.validation.annotations.number.validators.MaxDoubleValidator;
import gyro.plugin.validation.annotations.number.validators.MaxIntegerValidator;
import gyro.plugin.validation.annotations.number.validators.MaxLongValidator;
import gyro.plugin.validation.annotations.number.validators.MinDoubleValidator;
import gyro.plugin.validation.annotations.number.validators.MinIntegerValidator;
import gyro.plugin.validation.annotations.number.validators.MinLongValidator;
import gyro.plugin.validation.annotations.number.validators.RangeDoubleValidator;
import gyro.plugin.validation.annotations.number.validators.RangeIntegerValidator;
import gyro.plugin.validation.annotations.number.validators.RangeLongValidator;
import gyro.plugin.validation.annotations.string.validators.ListStringValidator;
import gyro.plugin.validation.annotations.string.validators.RegexStringValidator;
import gyro.plugin.validation.annotations.string.validators.ValidStringValidator;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationProcessor {
    private static Map<Class<?>, Object> validatorMap = new HashMap<>();

    static {
        validatorMap.put(RequiredValidator.class, new RequiredValidator());
        validatorMap.put(ValidStringValidator.class, new ValidStringValidator());
        validatorMap.put(ListStringValidator.class, new ListStringValidator());
        validatorMap.put(RegexStringValidator.class, new RegexStringValidator());
        validatorMap.put(RangeLongValidator.class, new RangeLongValidator());
        validatorMap.put(RangeDoubleValidator.class, new RangeDoubleValidator());
        validatorMap.put(RangeIntegerValidator.class, new RangeIntegerValidator());
        validatorMap.put(ListDoubleValidator.class, new ListDoubleValidator());
        validatorMap.put(ListLongValidator.class, new ListLongValidator());
        validatorMap.put(ListIntegerValidator.class, new ListIntegerValidator());
        validatorMap.put(MaxDoubleValidator.class, new MaxDoubleValidator());
        validatorMap.put(MaxLongValidator.class, new MaxLongValidator());
        validatorMap.put(MaxIntegerValidator.class, new MaxIntegerValidator());
        validatorMap.put(MinDoubleValidator.class, new MinDoubleValidator());
        validatorMap.put(MinLongValidator.class, new MinLongValidator());
        validatorMap.put(MinIntegerValidator.class, new MinIntegerValidator());
    }

    public static void validate(Diffable resource) {
        List<String> errorList = validateResource(resource, resource.primaryKey(), "");

        if (!errorList.isEmpty()) {
            throw new BeamException("\n" + String.join("\n", errorList));
        }
    }

    public static List<String> validationMessages(Diffable resource) {
        return validateResource(resource, resource.primaryKey(), "");
    }

    private static List<String> validateResource(Diffable resource, String resourceName, String indent) {
        List<String> errorList = new ArrayList<>();
        for (Method method : resource.getClass().getMethods()) {
            if (method.getName().startsWith("get")) {
                List<String> errors = validateMethod(method, resource, indent);
                errorList.addAll(errors);
            }
        }

        if (!errorList.isEmpty()) {
            errorList.add(0,String.format("\n%sx %s", indent, resourceName));
        }


        return errorList;
    }

    private static List<String> validateMethod(Method method, Diffable resource, String indent) {
        List<String> validationMessages = new ArrayList<>();

        String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, method.getName()).replace("get-", "");

        Object invokeObject = null;

        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(GyroValidation.class)) {
                AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
                if (annotationProcessorClass != null) {
                    AnnotationProcessor annotationProcessor = (AnnotationProcessor) validatorMap.get(annotationProcessorClass.value());
                    annotationProcessor.initialize(annotation);
                    try {
                        invokeObject = method.invoke(resource);

                        if (!annotationProcessor.isValid(invokeObject)) {
                            validationMessages.add(0,
                                String.format("%s· %s: %s. %s",indent, fieldName, invokeObject, annotationProcessor.getMessage()));
                            break;
                        }
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        if (invokeObject != null) {
            List<String> errorList = new ArrayList<>();
            if (invokeObject instanceof List) {
                List invokeList = (List) invokeObject;
                if (!invokeList.isEmpty() && invokeList.get(0) instanceof Diffable) {
                    for (Object invokeListObject : invokeList) {
                        Diffable diffableObject = (Diffable) invokeListObject;
                        errorList = validateResource(diffableObject, fieldName, indent + "    ");
                    }
                }
            } else {
                if (invokeObject instanceof Diffable) {
                    errorList = validateResource((Diffable) invokeObject, fieldName, indent + "    ");
                }
            }

            if (!errorList.isEmpty()) {
                validationMessages.addAll(errorList);
            }
        }

        return validationMessages;
    }
}
