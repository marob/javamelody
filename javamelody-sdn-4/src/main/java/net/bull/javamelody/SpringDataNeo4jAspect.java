package net.bull.javamelody;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author marob
 */
@Aspect
@Component
public class SpringDataNeo4jAspect {
    private static final Logger LOGGER = Logger.getLogger("javamelody");
    private final Counter neo4jCounter;

    public SpringDataNeo4jAspect() {
        neo4jCounter = MonitoringProxy.getNeo4jCounter();
    }

    /**
     * Pointcut
     *
     * @param proceedingJoinPoint ProceedingJoinPoint
     * @return Object
     * @throws Throwable Throwable
     */
    @Around("execution(* org.neo4j.ogm.session.Neo4jSession.*(..))")
    public Object openEntry(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (!neo4jCounter.isUsed()) {
            LOGGER.info("javamelody is monitoring Spring Data Neo4J Cypher queries");
            // Le compteur est affiché sauf si le paramètre displayed-counters dit le contraire
            neo4jCounter.setDisplayed(!Parameters.isCounterHidden(neo4jCounter.getName()));
            neo4jCounter.setUsed(true);
        }

        String methodName = proceedingJoinPoint.getSignature().getName();
        Object[] args = proceedingJoinPoint.getArgs();

        String requestName = "";

        if (Arrays.asList(new String[]{"execute", "query", "queryForObject"}).contains(methodName)) {
            if (args[0] instanceof String) {
                requestName = (String) args[0];
            } else if (args.length > 1 && args[1] instanceof String) {
                requestName = (String) args[1];
            }
        } else if ("load".equals(methodName)) {
            String className = ((Class) args[0]).getName();
            requestName = "load " + className + " by id";
        } else if ("loadAll".equals(methodName)) {
            if (args[0] instanceof Collection) {
                requestName = "load by instances";
            } else {
                String className = ((Class) args[0]).getName();
                if (args.length > 1 && args[1] instanceof Collection) {
                    requestName = "load " + className + " by ids";
                } else {
                    requestName = "load all " + className;
                }
            }
        } else if ("countEntitiesOfType".equals(methodName)) {
            String className = ((Class) args[0]).getName();
            requestName = "count entities of type " + className;
        } else if ("save".equals(methodName)) {
            String className = args[0].toString().split("\\{")[0];
            requestName = "save " + className;
        }

        //TODO: purgeDatabase, clear, delete, deleteAll

        if (!requestName.isEmpty()) {
            final long start = System.currentTimeMillis();
            boolean systemError = true;
            try {
                neo4jCounter.bindContext(requestName, requestName, null, -1);
                Object result = proceedingJoinPoint.proceed(args);
                systemError = false;
                return result;
            } catch (InvocationTargetException e) {
                // On lance l'exception originale afin que la fonction appelante ne soit pas consciente de l'existance de ce proxy
                // (sinon, une InvocationTargetException est lancée et l'appelant ne peut pas catcher l'exception d'origine)
                throw e.getCause();
            } finally {
                final long duration = Math.max(System.currentTimeMillis() - start, 0);
                neo4jCounter.addRequest(requestName, duration, -1, systemError, -1);
            }
        } else {
            try {
                return proceedingJoinPoint.proceed(args);
            } catch (InvocationTargetException e) {
                // On lance l'exception originale afin que la fonction appelante ne soit pas consciente de l'existance de ce proxy
                // (sinon, une InvocationTargetException est lancée et l'appelant ne peut pas catcher l'exception d'origine)
                throw e.getCause();
            }
        }
    }
}
