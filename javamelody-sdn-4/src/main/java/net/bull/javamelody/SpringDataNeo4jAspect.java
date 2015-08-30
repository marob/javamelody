package net.bull.javamelody;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author marob
 */
@Aspect
@Component
public class SpringDataNeo4jAspect {
    private static final Logger LOGGER = Logger.getLogger("javamelody");

    /**
     * Pointcut
     *
     * @param proceedingJoinPoint ProceedingJoinPoint
     * @return Object
     * @throws Throwable Throwable
     */
    @Around("execution(* org.springframework.data.neo4j.template.Neo4jOperations.query*())")
    public Object openEntry(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        LOGGER.debug(methodName);
        Object[] args = proceedingJoinPoint.getArgs();
        LOGGER.debug(args);
        Object result = proceedingJoinPoint.proceed(args);
        LOGGER.debug(result);
        return result;
    }
}
