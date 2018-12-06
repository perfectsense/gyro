package beam.aws;

import beam.core.BeamResource;
import beam.lang.BeamContextKey;
import beam.lang.BeamList;
import beam.lang.BeamLiteral;
import beam.lang.BeamScalar;
import com.google.common.base.CaseFormat;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.List;

@Aspect
public class BeamContextAspect {
    @Pointcut("target(resource) && set(private String *) && args(value)")
    void setString(BeamResource resource, Object value) {
    }

    @Pointcut("target(resource) && set(private Boolean *) && args(value)")
    void setBoolean(BeamResource resource, Object value) {
    }

    @Pointcut("target(resource) && set(private List *) && args(value)")
    void setList(BeamResource resource, Object value) {
    }

    @AfterReturning("setString(resource, value)")
    public void afterSetString(BeamResource resource, Object value, JoinPoint joinPoint) {
        if (value != null) {
            String key = joinPoint.getSignature().getName();
            key = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key);
            resource.addReferable(new BeamContextKey(key), new BeamLiteral(value.toString()));
        }
    }

    @AfterReturning("setBoolean(resource, value)")
    public void afterSetBoolean(BeamResource resource, Object value, JoinPoint joinPoint) {
        afterSetString(resource, value, joinPoint);
    }

    @AfterReturning("setList(resource, value)")
    public void afterSetList(BeamResource resource, Object value, JoinPoint joinPoint) {
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            BeamList beamList = new BeamList();
            for (Object item : list) {
                BeamScalar scalar = new BeamScalar();
                BeamLiteral literal = new BeamLiteral(item.toString());
                scalar.getElements().add(literal);
                beamList.getList().add(scalar);
            }

            String key = joinPoint.getSignature().getName();
            key = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key);
            resource.addReferable(new BeamContextKey(key), beamList);
        }
    }
}
