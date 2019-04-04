parser grammar BeamParser;

options { tokenVocab = BeamLexer; }

root
    :
    NEWLINE? statement
    (NEWLINE statement)*
    NEWLINE? EOF
    ;

statement
    : pair
    | resource
    | forStatement
    | ifStatement
    | importStmt
    | virtualResource
    ;

blockBody : (blockStatement NEWLINE)*;

blockStatement
    : pair
    | resource
    | forStatement
    | ifStatement
    ;

resource : resourceType resourceName? NEWLINE blockBody END;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | stringValue;

importStmt  : IMPORT importPath (AS importName)?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

virtualResource      : VR virtualResourceName NEWLINE virtualResourceParam* DEFINE NEWLINE blockBody END;
virtualResourceParam : PARAM IDENTIFIER NEWLINE;
virtualResourceName  : IDENTIFIER;

// -- Control Structures

forVariable : IDENTIFIER;

forStatement
    :
    FOR forVariable (COMMA forVariable)* IN (listValue | referenceValue) NEWLINE
        blockBody
    END
    ;

ifStatement
    :
    IF condition NEWLINE
        blockBody
    (ELSEIF condition NEWLINE
        blockBody)*
    (ELSE NEWLINE
        blockBody)?
    END
    ;

comparisonOperator : EQ | NOTEQ;

condition
    : value                             # ValueCondition
    | value comparisonOperator value    # ComparisonCondition
    | condition AND condition           # AndCondition
    | condition OR condition            # OrCondition
    ;

pair : key COLON value;

key
    : IDENTIFIER
    | STRING_LITERAL
    | IMPORT
    | AS
    | VR
    | PARAM
    | DEFINE
    ;

value
    : booleanValue
    | listValue
    | mapValue
    | numberValue
    | referenceValue
    | stringValue
    ;

booleanValue
    : TRUE
    | FALSE
    ;

listValue
    :
    LBRACKET NEWLINE?
        (value (COMMA NEWLINE?
        value)*       NEWLINE?)?
    RBRACKET
    ;

mapValue
    :
    LCURLY NEWLINE?
        (pair (COMMA NEWLINE?
        pair)*       NEWLINE?)?
    RCURLY
    ;

numberValue
    : DECIMAL_LITERAL
    | FLOAT_LITERAL
    ;

referenceValue
    : LREF resourceType referenceName (PIPE query)* (PIPE path)? RREF # ResourceReference
    | LREF path RREF                                                  # ValueReference
    ;

referenceName
    : GLOB | (IDENTIFIER SLASH GLOB)
    | resourceName
    ;

query
    : path comparisonOperator value # ComparisonQuery
    | query AND query               # AndQuery
    | query OR query                # OrQuery
    ;

path : IDENTIFIER (DOT IDENTIFIER)*;

stringValue : stringExpression | STRING_LITERAL;
stringExpression : DQUOTE stringContents* DQUOTE;
stringContents : TEXT | referenceValue;
