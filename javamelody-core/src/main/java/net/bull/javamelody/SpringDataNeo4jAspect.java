package net.bull.javamelody;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.MethodOverride;
import org.springframework.beans.factory.support.ReplaceOverride;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cglib.core.NamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.data.neo4j.core.*;
import org.springframework.data.neo4j.support.query.CypherQueryEngineImpl;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.objenesis.ObjenesisHelper;
import org.springframework.stereotype.Component;

import java.lang.reflect.*;

/**
 * @author marob
 */
@Aspect
@Component
public class SpringDataNeo4jAspect {

    private static final Class<?>[] CALLBACK_TYPES = new Class<?>[]
            {NoOp.class, LookupOverrideMethodInterceptor.class, NoOp.class};

    /*@Around("execution(* org.springframework.data.neo4j..*.*(..))")
    public Object test(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] parameterValues = proceedingJoinPoint.getArgs();

        proceedingJoinPoint.getSignature().getName();

        return proceedingJoinPoint.proceed(parameterValues);
    }*/

    /**
     * Pointcut
     *
     * @param proceedingJoinPoint ProceedingJoinPoint
     * @return Object
     * @throws Throwable Throwable
     */
    //@Around("execution(* org.springframework.data.neo4j.support.query.QueryEngine.query(String, java.util.Map))")
    //@Around("execution(* org.neo4j.graphdb.GraphDatabaseService.*(..))")
    //@Around("execution(* org.springframework.data.neo4j..*.*(..))")
    @Around("execution(* org.springframework.data.neo4j.config.Neo4jConfiguration.graphDatabase())")
    public Object openEntry(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] parameterValues = proceedingJoinPoint.getArgs();

        proceedingJoinPoint.getSignature().getName();

        proceedingJoinPoint.getTarget().getClass().getDeclaredFields();

        //String statement = (String) parameterValues[0];
        //Map<String, Object> params = (Map<String, Object>) parameterValues[1];

        Object result = proceedingJoinPoint.proceed(parameterValues);

        if (result instanceof GraphDatabase) {
            GraphDatabase graphDatabase = (GraphDatabase) result;

            // On cherche l'attribut QueryEngine par son type plutôt que par son nom afin d'être moins sensible aux changements
            Field queryEngineField = null;
            Field[] fields = graphDatabase.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (CypherQueryEngineImpl.class.isAssignableFrom(field.getType())) {
                    queryEngineField = field;
                    break;
                }
            }

            if (queryEngineField != null) {
                queryEngineField.setAccessible(true);
                CypherQueryEngineImpl queryEngine = (CypherQueryEngineImpl) queryEngineField.get(graphDatabase);

                //QueryEngine queryEngineProxy = generateQueryEngineProxy(queryEngine);
                CypherQueryEngineImpl queryEngineProxy = generateQueryEngineProxyWithCGLIB(queryEngine);

                queryEngineField.set(graphDatabase, queryEngineProxy);
            } else {
                // Fail
            }
        } else {
            // Fail
        }

        return result;
    }

    private CypherQueryEngineImpl generateQueryEngineProxyWithCGLIB(CypherQueryEngineImpl queryEngine) throws IllegalAccessException {
        Class<? extends QueryEngine> classToProxy = queryEngine.getClass();
        Enhancer enhancer = new Enhancer();
        //enhancer.setClassLoader(classToProxy.getClassLoader());
        enhancer.setSuperclass(CypherQueryEngineImpl.class);

        enhancer.setCallbackFilter(new MethodOverrideCallbackFilter());

        enhancer.setCallbackTypes(CALLBACK_TYPES);
        enhancer.setUseCache(false);

        // Instanciation d'une classe sans appel du constructeur (un appel à enhancer.create() n'est pas possible car CypherQueryEngineImpl n'a pas de contructeur vide)
        Class proxyClass = enhancer.createClass();
        CypherQueryEngineImpl queryEngineProxy = (CypherQueryEngineImpl) ObjenesisHelper.newInstance(proxyClass);
        // Comme aucun constructeur n'a été appelé, on doit renseigner à nouveau les attributs de la classe avec les valeurs de la classe initiale
        for (Field field : CypherQueryEngineImpl.class.getDeclaredFields()) {
            // On omet les champs static
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                field.set(queryEngineProxy, field.get(queryEngine));
            }
        }
        return queryEngineProxy;
    }

    // La création dynamique de poxy ne fonctionne pas parce que l'attribut contenant le QueryEngine est de type CypherQueryEngineImpl (une classe, pas une interface)
    /*private QueryEngine generateQueryEngineProxy(QueryEngine queryEngine) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchMethodException {
        Class<? extends QueryEngine> proxyClass = queryEngine.getClass();
        Class<?> queryEngineProxyClass = Proxy.getProxyClass(proxyClass.getClassLoader(), proxyClass.getInterfaces());

        QueryEngineInvocationHandler invocationHandler = new QueryEngineInvocationHandler(queryEngine);

        return (QueryEngine) queryEngineProxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(new Object[]{invocationHandler});
    }*/

    public class TestCallbackFilter implements CallbackFilter {
        @Override
        public int accept(Method method) {
            //if ("query".equals(method.getName())) return 1;
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            // TODO
            return super.equals(obj);
        }
    }

    public class TestMethodInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            return null;
        }
    }

    /**
     * CGLIB callback for filtering method interception behavior.
     */
    private static class MethodOverrideCallbackFilter implements CallbackFilter {
        private static final int PASSTHROUGH = 0;
        private static final int LOOKUP_OVERRIDE = 1;
        private static final int METHOD_REPLACER = 2;

        @Override
        public int accept(Method method) {
            if ("query".equals(method.getName())) return LOOKUP_OVERRIDE;
            return PASSTHROUGH;
            /*if (methodOverride == null) {
                return PASSTHROUGH;
            }
            else if (methodOverride instanceof LookupOverride) {
                return LOOKUP_OVERRIDE;
            }
            else if (methodOverride instanceof ReplaceOverride) {
                return METHOD_REPLACER;
            }
            throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
                    methodOverride.getClass().getName());*/
        }
    }

    /**
     * CGLIB MethodInterceptor to override methods, replacing them with an
     * implementation that returns a bean looked up in the container.
     */
    private static class LookupOverrideMethodInterceptor implements MethodInterceptor {

        /*private final BeanFactory owner;

        public LookupOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
            super(beanDefinition);
            this.owner = owner;
        }*/
        public LookupOverrideMethodInterceptor(){
            //
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
            // Cast is safe, as CallbackFilter filters are used selectively.
            //LookupOverride lo = (LookupOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
            Object[] argsToUse = (args.length > 0 ? args : null);  // if no-arg, don't insist on args at all
            return method.invoke(obj, args);
            /*if (StringUtils.hasText(lo.getBeanName())) {
                return this.owner.getBean(lo.getBeanName(), argsToUse);
            }
            else {
                return this.owner.getBean(method.getReturnType(), argsToUse);
            }*/
        }
    }
}
