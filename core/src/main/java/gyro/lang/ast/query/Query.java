package gyro.lang.ast.query;

import gyro.core.BeamException;
import gyro.core.query.QueryField;
import gyro.core.query.QueryType;
import gyro.lang.ExternalResourceQuery;
import gyro.lang.InternalResourceQuery;
import gyro.lang.Resource;
import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class Query {

    public static Query create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(BeamParser.QueryAndExpressionContext.class)) {
            return new AndQuery((BeamParser.QueryAndExpressionContext) context);

        } else if (cc.equals(BeamParser.QueryOrExpressionContext.class)) {
            return new OrQuery((BeamParser.QueryOrExpressionContext) context);

        } else if (cc.equals(BeamParser.QueryComparisonExpressionContext.class)) {
            return new ComparisonQuery((BeamParser.QueryComparisonExpressionContext) context);

        } else if (cc.equals(BeamParser.QueryFieldValueContext.class)) {
            return new FieldValueQuery((BeamParser.QueryFieldValueContext) context);

        }

        return null;
    }

    public static ExternalResourceQuery<Resource> createExternalResourceQuery(
        Scope scope,
        String type,
        String fieldName,
        String operator,
        Object value)
        throws Exception {

        try {
            ExternalResourceQuery<Resource> resourceQuery = createExternalResourceQuery(scope, type);
            resourceQuery.credentials(resourceQuery.resourceCredentials(scope));
            boolean validQuery = false;
            for (QueryField field : QueryType.getInstance(resourceQuery.getClass()).getFields()) {
                String key = field.getBeamName();
                if (fieldName.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)) {
                    validQuery = true;
                    field.setValue(resourceQuery, value);
                    resourceQuery.operator(operator);
                }
            }

            return validQuery ? resourceQuery : null;

        } catch (IllegalAccessException
            | InstantiationException
            | NoSuchMethodException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                ? (RuntimeException) cause
                : new RuntimeException(cause);
        }
    }

    public static ExternalResourceQuery<Resource> createExternalResourceQuery(Scope scope, String type) throws Exception {

        @SuppressWarnings("unchecked")
        Class<? extends ExternalResourceQuery> resourceQueryClass = (Class<? extends ExternalResourceQuery>) scope
            .getRootScope().getResourceQueryClasses().get(type);

        if (resourceQueryClass == null) {
            throw new BeamException("Resource type " + type + " does not support external queries.");
        }

        try {
            ExternalResourceQuery<Resource> resourceQuery = resourceQueryClass.getConstructor().newInstance();
            resourceQuery.credentials(resourceQuery.resourceCredentials(scope));
            return resourceQuery;

        } catch (IllegalAccessException
            | InstantiationException
            | NoSuchMethodException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                ? (RuntimeException) cause
                : new RuntimeException(cause);
        }
    }

    public static InternalResourceQuery<Resource> createInternalResourceQuery(
        Scope scope,
        String type,
        String fieldName,
        String operator,
        Object value)
        throws Exception {

        @SuppressWarnings("unchecked")
        Class<? extends Resource> resourceClass = (Class<? extends Resource>) scope.getRootScope().getResourceClasses().get(type);

        if (resourceClass == null) {
            throw new BeamException("Resource type " + type + " does not exist.");
        }

        return new InternalResourceQuery(type, resourceClass, fieldName, operator, value);
    }

    public abstract List<ResourceQueryGroup> evaluate(Scope scope, String type, boolean external) throws Exception;
}
