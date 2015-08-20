package net.bull.javamelody;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

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
    @Around("execution(* org.springframework.data.neo4j.config.Neo4jConfiguration.graphDatabase())")
    public Object openEntry(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] parameterValues = proceedingJoinPoint.getArgs();

        proceedingJoinPoint.getSignature().getName();

        proceedingJoinPoint.getTarget().getClass().getDeclaredFields();

        Object result = proceedingJoinPoint.proceed(parameterValues);

        if (result instanceof GraphDatabase) {
            GraphDatabase graphDatabase = (GraphDatabase) result;

            // On cherche l'attribut QueryEngine par son type plutôt que par son nom afin d'être moins sensible aux changements
            Field queryEngineField = null;
            Field[] fields = graphDatabase.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (QueryEngine.class.isAssignableFrom(field.getType())) {
                    queryEngineField = field;
                    break;
                }
            }

            if (queryEngineField != null) {
                queryEngineField.setAccessible(true);
                QueryEngine queryEngine = (QueryEngine) queryEngineField.get(graphDatabase);
                QueryEngine queryEngineProxy = generateQueryEngineProxy(queryEngine);
                queryEngineField.set(graphDatabase, queryEngineProxy);
            } else {
                LOGGER.warn("Couldn't find any field implementing org.springframework.data.neo4j.support.query.QueryEngine in org.springframework.data.neo4j.support.DelegatingGraphDatabase");
            }
        } else {
            LOGGER.warn("org.springframework.data.neo4j.config.Neo4jConfiguration.graphDatabase should return a org.springframework.data.neo4j.core.GraphDatabase");
        }

        return result;
    }

    private QueryEngine generateQueryEngineProxy(QueryEngine queryEngine) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchMethodException {
        Class<? extends QueryEngine> proxyClass = queryEngine.getClass();
        Class<?> queryEngineProxyClass = Proxy.getProxyClass(proxyClass.getClassLoader(), proxyClass.getInterfaces());

        QueryEngineInvocationHandler invocationHandler = new QueryEngineInvocationHandler(queryEngine);

        return (QueryEngine) queryEngineProxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(invocationHandler);
    }
}
