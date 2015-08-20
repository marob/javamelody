package net.bull.javamelody;

import org.springframework.data.neo4j.support.query.QueryEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class QueryEngineInvocationHandler implements InvocationHandler {
    private final QueryEngine queryEngine;
    private final Counter neo4jCounter;

    public QueryEngineInvocationHandler(QueryEngine queryEngine) {
        super();
        assert queryEngine != null;
        this.queryEngine = queryEngine;
        neo4jCounter = GraphWrapper.SINGLETON.getNeo4jCounter();
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
            } finally {
                final long duration = Math.max(System.currentTimeMillis() - start, 0);
                neo4jCounter.addRequest(requestName, duration, -1, systemError, -1);
            }
        } else {
            return method.invoke(queryEngine, args);
        }
    }
}
