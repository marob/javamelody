package net.bull.javamelody;

import org.apache.log4j.Logger;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GraphDatabaseInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = Logger.getLogger("javamelody");
    private final GraphDatabase graphDatabase;

    public GraphDatabaseInvocationHandler(GraphDatabase graphDatabase) {
        super();
        assert graphDatabase != null;
        this.graphDatabase = graphDatabase;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Object result;
        try {
            result = method.invoke(graphDatabase, args);
        } catch (InvocationTargetException e) {
            // On lance l'exception originale afin que la fonction appelante ne soit pas consciente de l'existance de ce proxy
            // (sinon, une InvocationTargetException est lancée et l'appelant ne peut pas catcher l'exception d'origine)
            throw e.getCause();
        }
        if ("queryEngine".equals(methodName)) {
            if (result instanceof QueryEngine) {
                result = generateQueryEngineProxy((QueryEngine) result);
                LOGGER.info("javamelody is monitoring Spring Data Neo4J Cypher queries");
            } else {
                LOGGER.error(graphDatabase.getClass().getName() + ".queryEngine should return a " + QueryEngine.class.getName());
            }
        }

        return result;
    }

    private QueryEngine generateQueryEngineProxy(QueryEngine queryEngine) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class<? extends QueryEngine> proxyClass = queryEngine.getClass();
        Class<?> queryEngineProxyClass = Proxy.getProxyClass(proxyClass.getClassLoader(), proxyClass.getInterfaces());

        QueryEngineInvocationHandler invocationHandler = new QueryEngineInvocationHandler(queryEngine);

        return (QueryEngine) queryEngineProxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(invocationHandler);
    }
}
