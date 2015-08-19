package net.bull.javamelody;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphWrapper {
    /**
     * Instance singleton de GraphWrapper
     */
    public static final GraphWrapper SINGLETON = new GraphWrapper(new Counter(
            Counter.NEO4J_COUNTER_NAME, "neo4j.png"));
    static final AtomicInteger ACTIVE_CONNECTION_COUNT = new AtomicInteger();

    // Cette variable neo4jCounter conserve un état qui est global au filtre et à l'application (donc thread-safe).
    private final Counter neo4jCounter;
    private boolean connectionInformationsEnabled;

    private GraphWrapper(Counter neo4jCounter) {
        super();
        assert neo4jCounter != null;
        this.neo4jCounter = neo4jCounter;
        connectionInformationsEnabled = Parameters.isSystemActionsEnabled()
                && !Parameters.isNoDatabase();
    }

    public Counter getNeo4jCounter() {
        return neo4jCounter;
    }

    public GraphDatabaseAPI createProxy(GraphDatabaseAPI graphDatabase) {
        assert graphDatabase != null;
        final GraphDatabaseServiceInvocationHandler invocationHandler = new GraphDatabaseServiceInvocationHandler(
                graphDatabase);
        return JdbcWrapperHelper.createProxy(graphDatabase, invocationHandler, null);
    }

    /**
     * Handler de proxy d'une connexion jdbc.
     */
    private class GraphDatabaseServiceInvocationHandler implements InvocationHandler {
        private final GraphDatabaseService graphDatabaseService;
        private boolean alreadyClosed;

        GraphDatabaseServiceInvocationHandler(GraphDatabaseService graphDatabaseService) {
            super();
            assert graphDatabaseService != null;
            this.graphDatabaseService = graphDatabaseService;
        }

        void init() {
            // on limite la taille pour éviter une éventuelle saturation mémoire
            /*if (isConnectionInformationsEnabled()
                    && USED_CONNECTION_INFORMATIONS.size() < MAX_USED_CONNECTION_INFORMATIONS) {
                USED_CONNECTION_INFORMATIONS.put(
                        ConnectionInformations.getUniqueIdOfConnection(connection),
                        new ConnectionInformations());
            }
            USED_CONNECTION_COUNT.incrementAndGet();
            TRANSACTION_COUNT.incrementAndGet();*/
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // performance : on évite method.invoke pour equals & hashCode
            final String methodName = method.getName();
            if (isEqualsMethod(methodName, args)) {
                return graphDatabaseService.equals(args[0]);
            } else if (isHashCodeMethod(methodName, args)) {
                return graphDatabaseService.hashCode();
            }
            try {
                // Remplacer par autre chose ? Dépends de la version de Neo4J ??
                if ("execute".equals(methodName)) {
                    String requestName = (String) args[0];
                    return doExecute(requestName, method, args);
                }
            } finally {
                if ("shutdown".equals(methodName) && !alreadyClosed) {
                    /*USED_CONNECTION_COUNT.decrementAndGet();
                    USED_CONNECTION_INFORMATIONS.remove(ConnectionInformations
                            .getUniqueIdOfConnection(connection));*/
                    alreadyClosed = true;
                }
            }
            return method.invoke(graphDatabaseService, args);
        }

        Object doExecute(String requestName, Method method, Object[] args)
                throws IllegalAccessException, InvocationTargetException {
            assert requestName != null;
            assert method != null;

            final long start = System.currentTimeMillis();
            boolean systemError = true;
            try {
                ACTIVE_CONNECTION_COUNT.incrementAndGet();

                // note perf: selon un paramètre current-sql(/requests)-disabled,
                // on pourrait ici ne pas binder un nouveau contexte à chaque requête sql
                neo4jCounter.bindContext(requestName, requestName, null, -1);

                final Object result = method.invoke(graphDatabaseService, args);
                systemError = false;
                return result;
            } finally {
                // Rq : on n'utilise pas la création du statement et l'appel à la méthode close du statement
                // comme début et fin d'une connexion active, car en fonction de l'application
                // la méthode close du statement peut ne jamais être appelée
                // (par exemple, seule la méthode close de la connection peut être appelée ce qui ferme aussi le statement)
                // Rq : pas de temps cpu pour les requêtes sql car c'est 0 ou quasiment 0
                ACTIVE_CONNECTION_COUNT.decrementAndGet();
                final long duration = Math.max(System.currentTimeMillis() - start, 0);
                neo4jCounter.addRequest(requestName, duration, -1, systemError, -1);
            }
        }
    }

    static boolean isEqualsMethod(Object methodName, Object[] args) {
        // == for perf (strings interned: == is ok)
        return "equals" == methodName && args != null && args.length == 1; // NOPMD
    }

    static boolean isHashCodeMethod(Object methodName, Object[] args) {
        // == for perf (strings interned: == is ok)
        return "hashCode" == methodName && (args == null || args.length == 0); // NOPMD
    }
}
