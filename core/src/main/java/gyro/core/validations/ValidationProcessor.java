package gyro.core.validations;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.diff.Diffable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        for (Annotation annotation : diffable.getClass().getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(AnnotationProcessorClass.class)) {
                String validationMessage = validateResourceAnnotation(annotation, diffable, indent);

                if (!ObjectUtils.isBlank(validationMessage)) {
                    errorList.add(validationMessage);
                    //break;
                }
            } else if (annotation.annotationType().isAnnotationPresent(RepeatableAnnotationProcessorClass.class)) {
                List<String> errorMessages = validateRepeatableAnnotation(annotation, diffable, indent);

                if (!errorMessages.isEmpty()) {
                    errorList.addAll(errorMessages);
                }
            }
        }

        if (!errorList.isEmpty()) {
            errorList.add(0,String.format("\n%sx %s", indent, resourceName));
        }

        return errorList;
    }

    private static List<String> validateMethod(Method method, Diffable diffable, String indent) {
        List<String> validationMessages = new ArrayList<>();

        Object invokeObject;

        try {
            invokeObject = method.invoke(diffable);

            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(AnnotationProcessorClass.class)) {
                    String validationMessage = validateFieldAnnotation(invokeObject,annotation,method, diffable, indent);
                    if (!ObjectUtils.isBlank(validationMessage)) {
                        validationMessages.add(0, validationMessage);
                        break;
                    }
                } else if (annotation.annotationType().isAnnotationPresent(RepeatableAnnotationProcessorClass.class)) {
                    List<String> errorMessages = validateRepeatableAnnotation(annotation, invokeObject, indent);

                    if (!errorMessages.isEmpty()) {
                        validationMessages.addAll(errorMessages);
                    }
                }
            }

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
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        return validationMessages;
    }

    private static String validateFieldAnnotation(Object invokeObject, Annotation annotation, Method method, Diffable diffable, String indent)
        throws IllegalAccessException, InvocationTargetException, IllegalArgumentException {
        String validationMessage = "";
        AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
        if (annotationProcessorClass != null) {
            if (!isValueReference(method, diffable) || invokeObject != null) {
                try {
                    AnnotationProcessor annotationProcessor = getAnnotationProcessor(annotation, annotationProcessorClass);
                    if (!annotationProcessor.isValid(invokeObject)) {
                        validationMessage = String.format("%s· %s: %s. %s", indent,
                            ValidationUtils.getFieldName(method.getName()), invokeObject, annotationProcessor.getMessage());
                    }
                } catch (ClassNotFoundException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return validationMessage;
    }

    private static String validateResourceAnnotation(Annotation annotation, Diffable diffable, String indent) {
        String validationMessage = "";
        try {
            AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
            if (annotationProcessorClass != null) {
                AnnotationProcessor annotationProcessor = getAnnotationProcessor(annotation, annotationProcessorClass);

                if (!annotationProcessor.isValid(diffable)) {
                    validationMessage = String.format("%s· %s", indent, annotationProcessor.getMessage());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException | ClassNotFoundException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        return validationMessage;
    }

    private static AnnotationProcessor getAnnotationProcessor(Annotation annotation, AnnotationProcessorClass annotationProcessorClass)
        throws IllegalAccessException, InvocationTargetException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException {
        Class<?> cls = Class.forName(annotationProcessorClass.value().getName());
        Method getProcessor = cls.getMethod("getAnnotationProcessor");
        AnnotationProcessor annotationProcessor = (AnnotationProcessor) getProcessor.invoke(cls);
        annotationProcessor.initialize(annotation);
        return annotationProcessor;
    }

    private static List<String> validateRepeatableAnnotation(Annotation annotation, Object object, String indent) {
        List<String> validationMessages = new ArrayList<>();
        try {
            RepeatableAnnotationProcessorClass annotationProcessorClass = annotation.annotationType()
                .getAnnotation(RepeatableAnnotationProcessorClass.class);
            if (annotationProcessorClass != null) {
                RepeatableAnnotationProcessor annotationProcessor = getRepeatableAnnotationProcessor(annotation, annotationProcessorClass);

                List<String> validations = (List<String>) annotationProcessor.getValidations(object);

                if (!validations.isEmpty()) {
                    validationMessages.addAll(validations.stream().map(o -> String.format("%s· %s", indent,o)).collect(Collectors.toList()));
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException | ClassNotFoundException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return validationMessages;
    }

    private static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor(Annotation annotation,
                                                                                  RepeatableAnnotationProcessorClass annotationProcessorClass)
        throws IllegalAccessException, InvocationTargetException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException {
        Class<?> cls = Class.forName(annotationProcessorClass.value().getName());
        Method getProcessor = cls.getMethod("getRepeatableAnnotationProcessor");
        RepeatableAnnotationProcessor repeatableAnnotationProcessor = (RepeatableAnnotationProcessor) getProcessor.invoke(cls);
        repeatableAnnotationProcessor.initialize(annotation);
        return repeatableAnnotationProcessor;
    }

    private static boolean isValueReference(Method method, Diffable diffable) {
        // find out if method returns null as it has a ref
        return false;
    }
}
