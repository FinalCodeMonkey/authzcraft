package com.fcm.authzcraft.pep.runtime;

import com.fcm.authzcraft.api.plan.PlanDecision;
import com.fcm.authzcraft.api.plan.RowFilterPlan;
import com.fcm.authzcraft.pep.autoconfigure.AuthzCraftPepProperties;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class AuthzCraftPepInterceptor implements Interceptor {

    private static final Log LOGGER = LogFactory.getLog(AuthzCraftPepInterceptor.class);

    private final AuthzCraftPepProperties properties;
    private final AuthzCraftPlanClient planClient;
    private final AuthzCraftRequesterResolver requesterResolver;
    private final RowFilterSqlCompiler sqlCompiler;

    public AuthzCraftPepInterceptor(AuthzCraftPepProperties properties,
                                    AuthzCraftPlanClient planClient,
                                    AuthzCraftRequesterResolver requesterResolver,
                                    RowFilterSqlCompiler sqlCompiler) {
        this.properties = properties;
        this.planClient = planClient;
        this.requesterResolver = requesterResolver;
        this.sqlCompiler = sqlCompiler;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (invocation.getArgs().length == 4) {
            return interceptFourArgQuery(invocation);
        }
        if (invocation.getArgs().length == 6) {
            return interceptSixArgQuery(invocation);
        }
        if (invocation.getArgs().length == 2) {
            return interceptUpdate(invocation);
        }
        return invocation.proceed();
    }

    private Object interceptFourArgQuery(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameterObject = invocation.getArgs()[1];
        RowBounds rowBounds = (RowBounds) invocation.getArgs()[2];
        ResultHandler<?> resultHandler = (ResultHandler<?>) invocation.getArgs()[3];
        BoundSql originalBoundSql = mappedStatement.getBoundSql(parameterObject);
        BoundSql newBoundSql = buildFilteredBoundSql(mappedStatement, originalBoundSql);
        if (newBoundSql == originalBoundSql) {
            return invocation.proceed();
        }
        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey = executor.createCacheKey(mappedStatement, parameterObject, rowBounds, newBoundSql);
        return executor.query(mappedStatement, parameterObject, rowBounds, resultHandler, cacheKey, newBoundSql);
    }

    private Object interceptSixArgQuery(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        BoundSql originalBoundSql = (BoundSql) invocation.getArgs()[5];
        BoundSql newBoundSql = buildFilteredBoundSql(mappedStatement, originalBoundSql);
        if (newBoundSql == originalBoundSql) {
            return invocation.proceed();
        }
        Executor executor = (Executor) invocation.getTarget();
        Object parameterObject = invocation.getArgs()[1];
        RowBounds rowBounds = (RowBounds) invocation.getArgs()[2];
        invocation.getArgs()[4] = executor.createCacheKey(mappedStatement, parameterObject, rowBounds, newBoundSql);
        invocation.getArgs()[5] = newBoundSql;
        return invocation.proceed();
    }

    private Object interceptUpdate(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameterObject = invocation.getArgs()[1];
        BoundSql originalBoundSql = mappedStatement.getBoundSql(parameterObject);
        BoundSql newBoundSql = buildFilteredBoundSql(mappedStatement, originalBoundSql);
        if (newBoundSql == originalBoundSql) {
            return invocation.proceed();
        }
        invocation.getArgs()[0] = cloneWithSql(mappedStatement, newBoundSql);
        return invocation.proceed();
    }

    private MappedStatement cloneWithSql(MappedStatement ms, final BoundSql newBoundSql) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId(),
                new SqlSource() {
                    @Override
                    public BoundSql getBoundSql(Object parameterObject) {
                        return newBoundSql;
                    }
                }, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.timeout(ms.getTimeout());
        builder.statementType(ms.getStatementType());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        builder.resultMaps(ms.getResultMaps());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        if (ms.getKeyColumns() != null && ms.getKeyColumns().length > 0) {
            builder.keyColumn(String.join(",", ms.getKeyColumns()));
        }
        builder.lang(ms.getLang());
        if (ms.getResultSets() != null && ms.getResultSets().length > 0) {
            builder.resultSets(String.join(",", ms.getResultSets()));
        }
        return builder.build();
    }

    private BoundSql buildFilteredBoundSql(MappedStatement mappedStatement, BoundSql originalBoundSql) {
        SqlCommandType sqlType = mappedStatement.getSqlCommandType();
        if (!SqlCommandType.SELECT.equals(sqlType) && !SqlCommandType.UPDATE.equals(sqlType) && !SqlCommandType.DELETE.equals(sqlType)) {
            return originalBoundSql;
        }
        AuthzCraftPepProperties.ResourceMapping mapping = resolveMapping(mappedStatement.getId());
        if (mapping == null) {
            return originalBoundSql;
        }
        long startedAt = System.nanoTime();
        try {
            AuthzCraftRequester requester = requesterResolver.resolveRequester();
            RowFilterPlan plan = planClient.plan(mappedStatement.getId(), mapping.getResourceKey(), mapping.getOperationCode(), requester);
            CompiledSqlFilter filter = sqlCompiler.compile(originalBoundSql.getSql(), plan, mapping);
            boolean sqlRewritten = !PlanDecision.ALLOW_ALL.equals(plan.getPlanDecision());
            publishExecutionHeaders(plan, mapping, requester);
            logExecution(mappedStatement.getId(), mapping, requester, plan, filter, sqlRewritten, startedAt);
            if (!sqlRewritten) {
                return originalBoundSql;
            }
            return assembleBoundSql(mappedStatement, originalBoundSql, filter);
        } catch (RuntimeException exception) {
            if (isFailOpen()) {
                LOGGER.warn("authzcraft pep fail-open mappedStatementId=" + mappedStatement.getId()
                        + " resourceKey=" + mapping.getResourceKey()
                        + " failureClass=" + exception.getClass().getName()
                        + " failure=" + exception.getMessage());
                return originalBoundSql;
            }
            throw exception;
        }
    }

    private boolean isFailOpen() {
        return "FAIL_OPEN".equalsIgnoreCase(properties.getFailureMode());
    }

    private void publishExecutionHeaders(RowFilterPlan plan,
                                         AuthzCraftPepProperties.ResourceMapping mapping,
                                         AuthzCraftRequester requester) {
        if (properties.getObservability() == null || !properties.getObservability().isResponseHeadersEnabled()) {
            return;
        }
        HttpServletResponse response = currentResponse();
        if (response == null || response.isCommitted()) {
            return;
        }
        setHeader(response, "X-AuthzCraft-Decision-Key", plan.getDecisionKey());
        setHeader(response, "X-AuthzCraft-Plan-Decision", plan.getPlanDecision() == null ? null : plan.getPlanDecision().name());
        setHeader(response, "X-AuthzCraft-Requester-Key", requester.getRequesterKey());
        setHeader(response, "X-AuthzCraft-Resource-Key", mapping.getResourceKey());
        setHeader(response, "X-AuthzCraft-Required-Access-Paths", plan.getRequiredAccessPathKeys() == null
                ? null : StringUtils.collectionToCommaDelimitedString(plan.getRequiredAccessPathKeys()));
        setHeader(response, "X-AuthzCraft-Failure-Code", plan.getFailureCode() == null ? null : plan.getFailureCode().name());
    }

    private HttpServletResponse currentResponse() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) attributes).getResponse();
    }

    private void setHeader(HttpServletResponse response, String name, String value) {
        if (StringUtils.hasText(value)) {
            response.setHeader(name, value);
        }
    }

    private void logExecution(String mappedStatementId,
                              AuthzCraftPepProperties.ResourceMapping mapping,
                              AuthzCraftRequester requester,
                              RowFilterPlan plan,
                              CompiledSqlFilter filter,
                              boolean sqlRewritten,
                              long startedAt) {
        if (properties.getObservability() == null || !properties.getObservability().isExecutionLogEnabled() || !LOGGER.isInfoEnabled()) {
            return;
        }
        long costMs = (System.nanoTime() - startedAt) / 1000000L;
        LOGGER.info("authzcraft pep execution mappedStatementId=" + mappedStatementId
                + " resourceKey=" + mapping.getResourceKey()
                + " requesterKind=" + requester.getRequesterKind()
                + " requesterKey=" + requester.getRequesterKey()
                + " decisionKey=" + plan.getDecisionKey()
                + " planDecision=" + plan.getPlanDecision()
                + " requiredAccessPathKeys=" + plan.getRequiredAccessPathKeys()
                + " failureCode=" + plan.getFailureCode()
                + " sqlRewritten=" + sqlRewritten
                + " addedParameterCount=" + filter.getParameters().size()
                + " costMs=" + costMs);
    }

    private AuthzCraftPepProperties.ResourceMapping resolveMapping(String mappedStatementId) {
        Map<String, AuthzCraftPepProperties.ResourceMapping> mappings = properties.getMappings();
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        AuthzCraftPepProperties.ResourceMapping mapping = mappings.get(mappedStatementId);
        if (mapping == null) {
            return null;
        }
        if (!StringUtils.hasText(mapping.getResourceKey())) {
            throw new AuthzCraftPepException("resourceKey is required for mappedStatementId=" + mappedStatementId);
        }
        if (!StringUtils.hasText(mapping.getOperationCode())) {
            mapping.setOperationCode("READ");
        }
        return mapping;
    }

    private BoundSql assembleBoundSql(MappedStatement mappedStatement, BoundSql originalBoundSql, CompiledSqlFilter filter) {
        List<ParameterMapping> authzMappings = new ArrayList<ParameterMapping>();
        List<String> authzKeys = new ArrayList<String>();
        for (int i = 0; i < filter.getParameters().size(); i++) {
            String key = "__authzcraft_param_" + i;
            authzKeys.add(key);
            authzMappings.add(new ParameterMapping.Builder(mappedStatement.getConfiguration(), key, Object.class).build());
        }
        List<ParameterMapping> newMappings = mergeMappings(originalBoundSql.getParameterMappings(), authzMappings, filter.getParameterMappingIndex());
        BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), filter.getSql(), newMappings, originalBoundSql.getParameterObject());
        copyAdditionalParameters(originalBoundSql, newBoundSql);
        for (int i = 0; i < authzKeys.size(); i++) {
            newBoundSql.setAdditionalParameter(authzKeys.get(i), filter.getParameters().get(i));
        }
        return newBoundSql;
    }

    private List<ParameterMapping> mergeMappings(List<ParameterMapping> original,
                                                 List<ParameterMapping> authzMappings,
                                                 int insertIndex) {
        List<ParameterMapping> merged = new ArrayList<ParameterMapping>();
        int boundedIndex = Math.max(0, Math.min(insertIndex, original.size()));
        merged.addAll(original.subList(0, boundedIndex));
        merged.addAll(authzMappings);
        merged.addAll(original.subList(boundedIndex, original.size()));
        return merged;
    }

    private void copyAdditionalParameters(BoundSql source, BoundSql target) {
        for (ParameterMapping mapping : source.getParameterMappings()) {
            String property = mapping.getProperty();
            if (source.hasAdditionalParameter(property)) {
                target.setAdditionalParameter(property, source.getAdditionalParameter(property));
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}