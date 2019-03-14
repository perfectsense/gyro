package gyro.core.validations;

import com.google.common.base.CaseFormat;
import gyro.core.diff.Diffable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ValidationProcessor {
    public static List<String> validationMessages(Diffable diffable) {
        return validateResource(diffable, diffable.primaryKey(), "");
    }

    private static List<String> validateResource(Diffable diffable, String resourceName, String indent) {
        List<String> errorList = new ArrayList<>();
        for (Method method : diffable.getClass().getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                List<String> errors = validateMethod(method, diffable, indent);
                errorList.addAll(errors);
            }
        }

        if (!errorList.isEmpty()) {
            errorList.add(0,String.format("\n%sx %s", indent, resourceName));
        }

        return errorList;
    }

    private static List<String> validateMethod(Method method, Diffable diffable, String indent) {
        List<String> validationMessages = new ArrayList<>();

        String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, method.getName()).replace("get-", "");

        Object invokeObject;

        try {
            invokeObject = method.invoke(diffable);

            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(AnnotationProcessorClass.class)) {
                    AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
                    if (annotationProcessorClass != null) {
                        if (invokeObject == null && !isValueReference(method, diffable)) {
                            try {
                                Class<?> cls = Class.forName(annotationProcessorClass.value().getName());
                                Method getProcessor = cls.getMethod("getAnnotationProcessor");
                                AnnotationProcessor annotationProcessor = (AnnotationProcessor) getProcessor.invoke(cls);
                                annotationProcessor.initialize(annotation);

                                if (!annotationProcessor.isValid(invokeObject)) {
                                    validationMessages.add(0,
                                        String.format("%s· %s: %s. %s", indent, fieldName, invokeObject, annotationProcessor.getMessage()));
                                    break;
                                }
                            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                                ex.printStackTrace();
                            }
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
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        return validationMessages;
    }

    private static boolean isValueReference(Method method, Diffable diffable) {
        // find out if method returns null as it has a ref
        return false;
    }
}
