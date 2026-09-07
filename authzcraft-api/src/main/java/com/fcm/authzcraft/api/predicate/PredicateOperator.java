package com.fcm.authzcraft.api.predicate;

public enum PredicateOperator {
    AND,
    OR,
    NOT,
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    IN,
    NOT_IN,
    IS_NULL,
    IS_NOT_NULL,
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS,
    EXISTS_PATH,
    TRUE,
    CUSTOM_AST
}