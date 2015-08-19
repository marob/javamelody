package net.bull.javamelody;

import org.springframework.data.neo4j.support.query.QueryEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class QueryEngineInvocationHandler implements InvocationHandler {
    private QueryEngine queryEngine;

    public QueryEngineInvocationHandler() {
        super();
    }

    public QueryEngineInvocationHandler(QueryEngine queryEngine) {
        super();
        assert queryEngine != null;
        this.queryEngine = queryEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        try {
            Object result = method.invoke(queryEngine, args);
            return result;
        } finally {
            // ?
        }
    }
}
