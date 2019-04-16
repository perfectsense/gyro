package gyro.core.validation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.diff.Diffable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ValidationProcessor {
    public static List<String> validationMessages(Diffable diffable) {
        return validateResource(diffable, diffable.primaryKey(), "");
    }

    private static List<String> validateResource(Diffable diffable, String resourceName, String indent) {
        List<String> validationMessages = new ArrayList<>();
        for (Method method : diffable.getClass().getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                List<String> validateFieldMessages = validateFields(method, diffable, indent);
                validationMessages.addAll(validateFieldMessages);
            }
        }

        List<String> customValidations = diffable.validations();

        validationMessages.addAll(customValidations.stream().map(message -> String.format("%s· %s", indent, message)).collect(Collectors.toList()));

        for (Method method : diffable.getClass().getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                List<String> validateComplexFieldMessages = validateComplexFields(method, diffable, indent);
                validationMessages.addAll(validateComplexFieldMessages);
            }
        }

        if (!validationMessages.isEmpty()) {
            validationMessages.add(0,String.format("\n%sx %s", indent, resourceName));
        }

        return validationMessages;
    }

    private static List<String> validateFields(Method method, Diffable diffable, String indent) {
        List<String> validationMessages = new ArrayList<>();

        try {
            Object invokeObject = method.invoke(diffable);

            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(AnnotationProcessorClass.class)) {
                    String validationMessage = validateFieldAnnotation(invokeObject, annotation, method, diffable, indent);
                    if (!ObjectUtils.isBlank(validationMessage)) {
                        validationMessages.add(validationMessage);
                        break;
                    }
                } else if (annotation.annotationType().isAnnotationPresent(RepeatableAnnotationProcessorClass.class)) {
                    List<String> errorMessages = validateRepeatableAnnotation(annotation, invokeObject, indent, method);

                    if (!errorMessages.isEmpty()) {
                        validationMessages.addAll(errorMessages);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        return validationMessages;
    }

    private static List<String> validateComplexFields(Method method, Diffable diffable, String indent) {
        List<String> validationMessages = new ArrayList<>();

        try {
            Object invokeObject = method.invoke(diffable);

            if (invokeObject != null) {
                String fieldName = ValidationUtils.getFieldName(method.getName());

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

        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }

        return validationMessages;
    }

    private static String validateFieldAnnotation(Object invokeObject, Annotation annotation, Method method, Diffable diffable, String indent)
        throws IllegalArgumentException {
        String validationMessage = "";
        AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
        if (annotationProcessorClass != null) {
            if (!isValueReference(method, diffable) || invokeObject != null) {
                try {
                    Validator validator = (Validator) SINGLETONS.get(annotationProcessorClass.value());
                    if (!validator.isValid(annotation, invokeObject)) {
                        validationMessage = String.format("%s· %s: %s. %s", indent,
                            ValidationUtils.getFieldName(method.getName()), invokeObject, validator.getMessage());
                    }
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return validationMessage;
    }

    private static List<String> validateRepeatableAnnotation(Annotation annotation, Object object, String indent, Method method) {
        List<String> validationMessages = new ArrayList<>();
        try {
            RepeatableAnnotationProcessorClass annotationProcessorClass = annotation.annotationType()
                .getAnnotation(RepeatableAnnotationProcessorClass.class);
            if (annotationProcessorClass != null) {
                RepeatableValidator annotationProcessor = (RepeatableValidator) SINGLETONS.get(annotationProcessorClass.value());

                List<String> validations = (List<String>) annotationProcessor.getValidations(annotation, object);

                if (!validations.isEmpty()) {
                    if (method != null) {
                        validationMessages.addAll(validations.stream().map(o -> String.format("%s· %s: %s. %s", indent,
                            ValidationUtils.getFieldName(method.getName()), object, o)).collect(Collectors.toList()));
                    } else {
                        validationMessages.addAll(validations.stream().map(o -> String.format("%s· %s", indent, o)).collect(Collectors.toList()));
                    }
                }
            }
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
        return validationMessages;
    }

    private static boolean isValueReference(Method method, Diffable diffable) {
        // find out if method returns null as it has a ref
        return false;
    }

    private static final LoadingCache<Class<?>, Object> SINGLETONS = CacheBuilder.newBuilder()
        .weakKeys()
        .build(new CacheLoader<Class<?>, Object>() {
            public Object load(Class<?> c) throws IllegalAccessException, InstantiationException {
                return c.newInstance();
            }
        });
}
