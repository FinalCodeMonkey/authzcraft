package com.fcm.authzcraft.api.predicate;

import java.util.ArrayList;
import java.util.List;

public class PredicateNode {

    private PredicateOperator operator;
    private List<PredicateNode> operands = new ArrayList<PredicateNode>();
    private PredicateExpression left;
    private PredicateExpression right;
    private String accessPathKey;
    private PredicateNode pathPredicate;
    private Object customAst;

    public PredicateNode() {
    }

    public PredicateNode(PredicateOperator operator) {
        this.operator = operator;
    }

    public PredicateOperator getOperator() {
        return operator;
    }

    public void setOperator(PredicateOperator operator) {
        this.operator = operator;
    }

    public List<PredicateNode> getOperands() {
        return operands;
    }

    public void setOperands(List<PredicateNode> operands) {
        this.operands = operands;
    }

    public PredicateExpression getLeft() {
        return left;
    }

    public void setLeft(PredicateExpression left) {
        this.left = left;
    }

    public PredicateExpression getRight() {
        return right;
    }

    public void setRight(PredicateExpression right) {
        this.right = right;
    }

    public String getAccessPathKey() {
        return accessPathKey;
    }

    public void setAccessPathKey(String accessPathKey) {
        this.accessPathKey = accessPathKey;
    }

    public PredicateNode getPathPredicate() {
        return pathPredicate;
    }

    public void setPathPredicate(PredicateNode pathPredicate) {
        this.pathPredicate = pathPredicate;
    }

    public Object getCustomAst() {
        return customAst;
    }

    public void setCustomAst(Object customAst) {
        this.customAst = customAst;
    }
}