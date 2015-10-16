package net.bull.javamelody;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GraphDatabaseInvocationHandler implements InvocationHandler {
	private static final Logger LOGGER = Logger.getLogger("javamelody");
	private final GraphDatabase graphDatabase;
	private final Counter neo4jCounter;

	public GraphDatabaseInvocationHandler(GraphDatabase graphDatabase) {
		super();
		assert graphDatabase != null;
		this.graphDatabase = graphDatabase;
		neo4jCounter = MonitoringProxy.getNeo4jCounter();
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (!neo4jCounter.isUsed()) {
			LOGGER.info("javamelody is monitoring Spring Data Neo4J Cypher queries");
			// Le compteur est affiché sauf si le paramètre displayed-counters dit le contraire
			neo4jCounter.setDisplayed(!Parameters.isCounterHidden(neo4jCounter.getName()));
			neo4jCounter.setUsed(true);
		}

		String methodName = method.getName();
		String requestName = "";
		if ("getNodeById".equals(methodName)) {
			requestName = "load by id";
		} else if ("createNode".equals(methodName)) {
			requestName = "create node: " + args[1];
		} else if ("merge".equals(methodName)) {
			requestName = "merge node";
		} else if ("getOrCreateNode".equals(methodName)) {
			requestName = "get or create node";
		} else if ("getRelationshipById".equals(methodName)) {
			requestName = "get relationship by id";
		} else if ("createRelationship".equals(methodName)) {
			requestName = "create relationship";
		} else if ("getOrCreateRelationship".equals(methodName)) {
			requestName = "get or create relationship";
		} else if ("remove".equals(methodName)) {
			if (args[0] instanceof Node) {
				requestName = "remove node";
			}
			if (args[0] instanceof Relationship) {
				requestName = "remove relationship";
			}
		}
		if (!requestName.isEmpty()) {
			neo4jCounter.bindContext(requestName, requestName, null, -1);
		}
		final long start = System.currentTimeMillis();
		boolean systemError = true;
		try {
			Object result = method.invoke(graphDatabase, args);
			systemError = false;
			if ("queryEngine".equals(methodName)) {
				if (result instanceof QueryEngine) {
					result = generateQueryEngineProxy((QueryEngine) result);
				} else {
					LOGGER.error(
							graphDatabase.getClass().getName() + ".queryEngine should return a " + QueryEngine.class
									.getName());
				}
			}
			return result;
		} catch (InvocationTargetException e) {
			// On lance l'exception originale afin que la fonction appelante ne soit pas consciente de l'existance de ce proxy
			// (sinon, une InvocationTargetException est lancée et l'appelant ne peut pas catcher l'exception d'origine)
			throw e.getCause();
		} finally {
			if (!requestName.isEmpty()) {
				final long duration = Math.max(System.currentTimeMillis() - start, 0);
				neo4jCounter.addRequest(requestName, duration, -1, systemError, -1);
			}
		}
	}

	private QueryEngine generateQueryEngineProxy(QueryEngine queryEngine)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Class<? extends QueryEngine> proxyClass = queryEngine.getClass();
		Class<?> queryEngineProxyClass = Proxy.getProxyClass(proxyClass.getClassLoader(), proxyClass.getInterfaces());
		QueryEngineInvocationHandler invocationHandler = new QueryEngineInvocationHandler(queryEngine);
		return (QueryEngine) queryEngineProxyClass.getConstructor(new Class[] { InvocationHandler.class })
				.newInstance(invocationHandler);
	}
}
