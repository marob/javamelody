package net.bull.javamelody;

import org.springframework.data.neo4j.support.query.QueryEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class QueryEngineInvocationHandler implements InvocationHandler {
    private final QueryEngine queryEngine;
    private final Counter neo4jCounter;

    public QueryEngineInvocationHandler(QueryEngine queryEngine) {
        super();
        assert queryEngine != null;
        this.queryEngine = queryEngine;
        neo4jCounter = MonitoringProxy.getNeo4jCounter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("query".equals(methodName)) {
            String requestName = (String) args[0];
            final long start = System.currentTimeMillis();
            boolean systemError = true;
            try {
                neo4jCounter.bindContext(requestName, requestName, null, -1);
                final Object result = method.invoke(queryEngine, args);
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
                return method.invoke(queryEngine, args);
            } catch (InvocationTargetException e) {
                // On lance l'exception originale afin que la fonction appelante ne soit pas consciente de l'existance de ce proxy
                // (sinon, une InvocationTargetException est lancée et l'appelant ne peut pas catcher l'exception d'origine)
                throw e.getCause();
            }
        }
    }
}
