package com.fcm.authzcraft.pep.runtime;

import com.fcm.authzcraft.api.plan.BindingValue;
import com.fcm.authzcraft.api.plan.PlanDecision;
import com.fcm.authzcraft.api.plan.RowFilterPlan;
import com.fcm.authzcraft.api.predicate.PredicateExpression;
import com.fcm.authzcraft.api.predicate.PredicateExpressionKind;
import com.fcm.authzcraft.api.predicate.PredicateNode;
import com.fcm.authzcraft.api.predicate.PredicateOperator;
import com.fcm.authzcraft.pep.autoconfigure.AuthzCraftPepProperties;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RowFilterSqlCompiler {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bwhere\\b");
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(order\\s+by|limit|offset|fetch)\\b");

    public CompiledSqlFilter compile(String sql,
                                     RowFilterPlan plan,
                                     AuthzCraftPepProperties.ResourceMapping mapping) {
        String upperSql = StringUtils.hasText(sql) ? sql.trim().toUpperCase() : "";
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE")) {
            throw new AuthzCraftPepException("AuthzCraft PEP only supports SELECT, UPDATE, and DELETE in current MVP");
        }
        if (PlanDecision.ALLOW_ALL.equals(plan.getPlanDecision())) {
            return new CompiledSqlFilter(sql, Collections.<Object>emptyList(), countPlaceholders(sql));
        }
        if (PlanDecision.DENY_ALL.equals(plan.getPlanDecision())) {
            return injectCondition(sql, new SqlFragment("1 = 0", Collections.<Object>emptyList()));
        }
        if (!PlanDecision.FILTER.equals(plan.getPlanDecision())) {
            throw new AuthzCraftPepException("PDP returned fail-close decision: " + plan.getPlanDecision() + ", failureCode=" + plan.getFailureCode());
        }
        validateRequiredAccessPaths(plan, mapping);
        SqlFragment predicate = compilePredicate(plan.getPredicate(), plan.getBindings(), mapping, 0);
        return injectCondition(sql, predicate);
    }

    private SqlFragment compilePredicate(PredicateNode node,
                                         Map<String, BindingValue> bindings,
                                         AuthzCraftPepProperties.ResourceMapping mapping,
                                         int pathDepth) {
        if (node == null || node.getOperator() == null) {
            throw new AuthzCraftPepException("PDP FILTER plan has no predicate");
        }
        PredicateOperator operator = node.getOperator();
        if (PredicateOperator.EXISTS_PATH.equals(operator) || StringUtils.hasText(node.getAccessPathKey())) {
            return compileExistsPath(node, bindings, mapping, pathDepth);
        }
        if (PredicateOperator.AND.equals(operator) || PredicateOperator.OR.equals(operator)) {
            List<SqlFragment> parts = new ArrayList<SqlFragment>();
            for (PredicateNode operand : node.getOperands()) {
                parts.add(compilePredicate(operand, bindings, mapping, pathDepth));
            }
            return join(operator.name(), parts);
        }
        if (PredicateOperator.NOT.equals(operator)) {
            if (node.getOperands().size() != 1) {
                throw new AuthzCraftPepException("NOT predicate must have exactly one operand");
            }
            SqlFragment inner = compilePredicate(node.getOperands().get(0), bindings, mapping, pathDepth);
            return new SqlFragment("NOT (" + inner.getSql() + ")", inner.getParameters());
        }
        if (PredicateOperator.IS_NULL.equals(operator) || PredicateOperator.IS_NOT_NULL.equals(operator)) {
            String field = compileField(node.getLeft(), mapping);
            return new SqlFragment(field + " " + operatorSql(operator), Collections.<Object>emptyList());
        }
        if (PredicateOperator.TRUE.equals(operator)) {
            return new SqlFragment("1 = 1", Collections.<Object>emptyList());
        }
        if (PredicateOperator.IN.equals(operator) || PredicateOperator.NOT_IN.equals(operator)) {
            String field = compileField(node.getLeft(), mapping);
            List<Object> values = compileValues(node.getRight(), bindings);
            if (values.isEmpty()) {
                return new SqlFragment(PredicateOperator.IN.equals(operator) ? "1 = 0" : "1 = 1", Collections.<Object>emptyList());
            }
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    placeholders.append(", ");
                }
                placeholders.append("?");
            }
            return new SqlFragment(field + " " + operatorSql(operator) + " (" + placeholders + ")", values);
        }
        if (PredicateOperator.STARTS_WITH.equals(operator)
                || PredicateOperator.ENDS_WITH.equals(operator)
                || PredicateOperator.CONTAINS.equals(operator)) {
            String field = compileField(node.getLeft(), mapping);
            Object value = singleValue(node.getRight(), bindings);
            return new SqlFragment(field + " LIKE ?", Collections.<Object>singletonList(patternValue(operator, value)));
        }
        String field = compileField(node.getLeft(), mapping);
        Object value = singleValue(node.getRight(), bindings);
        return new SqlFragment(field + " " + operatorSql(operator) + " ?", Collections.<Object>singletonList(value));
    }

    private SqlFragment compileExistsPath(PredicateNode node,
                                          Map<String, BindingValue> bindings,
                                          AuthzCraftPepProperties.ResourceMapping mapping,
                                          int pathDepth) {
        String accessPathKey = node.getAccessPathKey();
        if (!StringUtils.hasText(accessPathKey)) {
            throw new AuthzCraftPepException("EXISTS_PATH predicate must declare accessPathKey");
        }
        AuthzCraftPepProperties.AccessPathMapping accessPath = mapping.getAccessPaths() == null
                ? null : mapping.getAccessPaths().get(accessPathKey);
        if (accessPath == null) {
            throw new AuthzCraftPepException("Access path mapping is missing: " + accessPathKey);
        }
        PredicateNode pathPredicate = PredicateOperator.EXISTS_PATH.equals(node.getOperator())
                ? node.getPathPredicate() : withoutAccessPath(node);
        if (pathPredicate == null) {
            throw new AuthzCraftPepException("EXISTS_PATH predicate must declare pathPredicate");
        }
        String alias = "__authz_path_" + (pathDepth + 1);
        String targetTable = requireIdentifier(accessPath.getTargetTable(), "accessPath.targetTable");
        String sourceField = requireIdentifier(accessPath.getSourceField(), "accessPath.sourceField");
        String targetField = requireSimpleIdentifier(accessPath.getTargetField(), "accessPath.targetField");
        AuthzCraftPepProperties.ResourceMapping pathMapping = toPathResourceMapping(accessPath, alias);
        SqlFragment predicate = compilePredicate(pathPredicate, bindings, pathMapping, pathDepth + 1);
        String correlation = alias + "." + targetField + " = " + sourceField;
        return new SqlFragment("EXISTS (SELECT 1 FROM " + targetTable + " " + alias
                + " WHERE " + correlation + " AND (" + predicate.getSql() + "))", predicate.getParameters());
    }

    private PredicateNode withoutAccessPath(PredicateNode node) {
        PredicateNode copy = new PredicateNode();
        copy.setOperator(node.getOperator());
        copy.setLeft(node.getLeft());
        copy.setRight(node.getRight());
        copy.setPathPredicate(node.getPathPredicate());
        copy.setCustomAst(node.getCustomAst());
        List<PredicateNode> operands = new ArrayList<PredicateNode>();
        for (PredicateNode operand : node.getOperands()) {
            operands.add(withoutAccessPath(operand));
        }
        copy.setOperands(operands);
        return copy;
    }

    private AuthzCraftPepProperties.ResourceMapping toPathResourceMapping(AuthzCraftPepProperties.AccessPathMapping accessPath,
                                                                          String alias) {
        AuthzCraftPepProperties.ResourceMapping pathMapping = new AuthzCraftPepProperties.ResourceMapping();
        Map<String, String> fieldMappings = new LinkedHashMap<String, String>();
        if (accessPath.getTargetFieldMappings() != null) {
            for (Map.Entry<String, String> entry : accessPath.getTargetFieldMappings().entrySet()) {
                String column = requireIdentifier(entry.getValue(), "accessPath.targetFieldMappings." + entry.getKey());
                fieldMappings.put(entry.getKey(), column.indexOf('.') >= 0 ? column : alias + "." + column);
            }
        }
        pathMapping.setFieldMappings(fieldMappings);
        return pathMapping;
    }

    private void validateRequiredAccessPaths(RowFilterPlan plan, AuthzCraftPepProperties.ResourceMapping mapping) {
        if (plan.getRequiredAccessPathKeys() == null || plan.getRequiredAccessPathKeys().isEmpty()) {
            return;
        }
        for (String accessPathKey : plan.getRequiredAccessPathKeys()) {
            if (mapping.getAccessPaths() == null || !mapping.getAccessPaths().containsKey(accessPathKey)) {
                throw new AuthzCraftPepException("Access path mapping is missing: " + accessPathKey);
            }
        }
    }

    private SqlFragment join(String operator, List<SqlFragment> parts) {
        if (parts.isEmpty()) {
            throw new AuthzCraftPepException(operator + " predicate must have operands");
        }
        List<Object> parameters = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sql.append(" ").append(operator).append(" ");
            }
            sql.append("(").append(parts.get(i).getSql()).append(")");
            parameters.addAll(parts.get(i).getParameters());
        }
        return new SqlFragment(sql.toString(), parameters);
    }

    private String compileField(PredicateExpression expression, AuthzCraftPepProperties.ResourceMapping mapping) {
        if (expression == null || !PredicateExpressionKind.FIELD.equals(expression.getKind())) {
            throw new AuthzCraftPepException("Predicate left expression must be FIELD");
        }
        String fieldKey = expression.getFieldKey();
        String column = mapping.getFieldMappings() == null ? null : mapping.getFieldMappings().get(fieldKey);
        String resolved = StringUtils.hasText(column) ? column : fieldKey;
        return requireIdentifier(resolved, "field mapping");
    }

    private String requireIdentifier(String value, String fieldName) {
        if (!StringUtils.hasText(value) || !IDENTIFIER.matcher(value).matches()) {
            throw new AuthzCraftPepException("Unsafe " + fieldName + ": " + value);
        }
        return value;
    }

    private String requireSimpleIdentifier(String value, String fieldName) {
        if (!StringUtils.hasText(value) || !SIMPLE_IDENTIFIER.matcher(value).matches()) {
            throw new AuthzCraftPepException("Unsafe " + fieldName + ": " + value);
        }
        return value;
    }

    private List<Object> compileValues(PredicateExpression expression, Map<String, BindingValue> bindings) {
        Object value = expressionValue(expression, bindings);
        if (value instanceof Collection) {
            return new ArrayList<Object>((Collection<?>) value);
        }
        List<Object> values = new ArrayList<Object>();
        values.add(value);
        return values;
    }

    private Object singleValue(PredicateExpression expression, Map<String, BindingValue> bindings) {
        Object value = expressionValue(expression, bindings);
        if (value instanceof Collection) {
            Collection<?> values = (Collection<?>) value;
            if (values.size() != 1) {
                throw new AuthzCraftPepException("Predicate requires a single value but got collection size=" + values.size());
            }
            return values.iterator().next();
        }
        return value;
    }

    private Object expressionValue(PredicateExpression expression, Map<String, BindingValue> bindings) {
        if (expression == null || expression.getKind() == null) {
            throw new AuthzCraftPepException("Predicate value expression is missing");
        }
        if (PredicateExpressionKind.BINDING.equals(expression.getKind())) {
            BindingValue binding = bindings == null ? null : bindings.get(expression.getBindingKey());
            if (binding == null) {
                throw new AuthzCraftPepException("Plan binding is missing: " + expression.getBindingKey());
            }
            return binding.getValue();
        }
        if (PredicateExpressionKind.LITERAL.equals(expression.getKind())) {
            return expression.getLiteralValue();
        }
        throw new AuthzCraftPepException("Unsupported predicate value expression kind: " + expression.getKind());
    }

    private CompiledSqlFilter injectCondition(String sql, SqlFragment condition) {
        int suffixIndex = findSuffixIndex(sql);
        String head = suffixIndex < 0 ? sql : sql.substring(0, suffixIndex);
        String suffix = suffixIndex < 0 ? "" : sql.substring(suffixIndex);
        int mappingIndex = countPlaceholders(head);
        String connector = hasWhere(head) ? " AND " : " WHERE ";
        return new CompiledSqlFilter(head + connector + "(" + condition.getSql() + ")" + suffix,
                condition.getParameters(), mappingIndex);
    }

    private boolean hasWhere(String sql) {
        return WHERE_PATTERN.matcher(sql).find();
    }

    private int findSuffixIndex(String sql) {
        Matcher matcher = SUFFIX_PATTERN.matcher(sql);
        return matcher.find() ? matcher.start() : -1;
    }

    private int countPlaceholders(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    private String operatorSql(PredicateOperator operator) {
        if (PredicateOperator.EQ.equals(operator)) {
            return "=";
        }
        if (PredicateOperator.NE.equals(operator)) {
            return "<>";
        }
        if (PredicateOperator.GT.equals(operator)) {
            return ">";
        }
        if (PredicateOperator.GE.equals(operator)) {
            return ">=";
        }
        if (PredicateOperator.LT.equals(operator)) {
            return "<";
        }
        if (PredicateOperator.LE.equals(operator)) {
            return "<=";
        }
        if (PredicateOperator.IN.equals(operator)) {
            return "IN";
        }
        if (PredicateOperator.NOT_IN.equals(operator)) {
            return "NOT IN";
        }
        if (PredicateOperator.IS_NULL.equals(operator)) {
            return "IS NULL";
        }
        if (PredicateOperator.IS_NOT_NULL.equals(operator)) {
            return "IS NOT NULL";
        }
        throw new AuthzCraftPepException("Unsupported predicate operator: " + operator);
    }

    private Object patternValue(PredicateOperator operator, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (PredicateOperator.STARTS_WITH.equals(operator)) {
            return text + "%";
        }
        if (PredicateOperator.ENDS_WITH.equals(operator)) {
            return "%" + text;
        }
        return "%" + text + "%";
    }
}