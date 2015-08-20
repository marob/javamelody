package net.bull.javamelody;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
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
        Object[] args = proceedingJoinPoint.getArgs();
        Object result = proceedingJoinPoint.proceed(args);
        if (result instanceof GraphDatabase) {
            result = generateGraphDatabaseProxy((GraphDatabase) result);
        } else {
            LOGGER.warn("org.springframework.data.neo4j.config.Neo4jConfiguration.graphDatabase should return a " + GraphDatabase.class.getName());
        }
        return result;
    }

    private GraphDatabase generateGraphDatabaseProxy(GraphDatabase graphDatabase) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<? extends GraphDatabase> proxyClass = graphDatabase.getClass();
        Class<?> graphDatabaseProxyClass = Proxy.getProxyClass(proxyClass.getClassLoader(), proxyClass.getInterfaces());

        GraphDatabaseInvocationHandler invocationHandler = new GraphDatabaseInvocationHandler(graphDatabase);

        return (GraphDatabase) graphDatabaseProxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(invocationHandler);
    }
}
