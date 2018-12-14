package beam.lang;

import com.google.common.base.CaseFormat;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.List;

@Aspect
public class BeamContextAspect {

    @Pointcut("this(config) && set(private String *) && args(value)")
    void setString(BeamBlock config, Object value) {
    }

    @Pointcut("this(config) && set(private Boolean *) && args(value)")
    void setBoolean(BeamBlock config, Object value) {
    }

    @Pointcut("this(config) && set(private List *) && args(value)")
    void setList(BeamBlock config, Object value) {
    }

    @AfterReturning("setString(config, value)")
    public void afterSetString(BeamBlock config, Object value, JoinPoint joinPoint) {
        if (value != null) {
            String key = joinPoint.getSignature().getName();
            key = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key);
            config.add(new BeamContextKey(key), new BeamLiteral(value.toString()));
        }
    }

    @AfterReturning("setBoolean(config, value)")
    public void afterSetBoolean(BeamBlock config, Object value, JoinPoint joinPoint) {
        afterSetString(config, value, joinPoint);
    }

    @AfterReturning("setList(config, value)")
    public void afterSetList(BeamBlock config, Object value, JoinPoint joinPoint) {
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
            config.add(new BeamContextKey(key), beamList);
        }
    }

}
