package gyro.core.validation;

import com.google.common.base.CaseFormat;
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
                String validationMessage = validateFields(method, diffable, indent);
                if (!ObjectUtils.isBlank(validationMessage)) {
                    validationMessages.add(validationMessage);
                }
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

    private static String validateFields(Method method, Diffable diffable, String indent) {
        String validationMessage = "";

        try {
            Object invokeObject = method.invoke(diffable);

            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(AnnotationProcessorClass.class)) {
                    AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
                    if (annotationProcessorClass != null) {
                        if (!isValueReference(method, diffable) || invokeObject != null) {
                            try {
                                Validator validator = (Validator) SINGLETONS.get(annotationProcessorClass.value());
                                if (!validator.isValid(annotation, invokeObject)) {
                                    validationMessage = String.format("%s· %s: %s. %s", indent,
                                        getFieldName(method.getName()), invokeObject, validator.getMessage(annotation));
                                }
                            } catch (ExecutionException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        return validationMessage;
    }

    private static List<String> validateComplexFields(Method method, Diffable diffable, String indent) {
        List<String> validationMessages = new ArrayList<>();

        try {
            Object invokeObject = method.invoke(diffable);

            if (invokeObject != null) {
                String fieldName = getFieldName(method.getName());

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

    static String getFieldName(String fieldName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, fieldName).replace("get-", "");
    }
}
