parser grammar BeamParser;

options { tokenVocab = BeamLexer; }

root
    :
    NEWLINES? statement
    (NEWLINES statement)*
    NEWLINES? EOF
    ;

statement
    : importStmt
    | resource
    | virtualResource
    | forStatement
    | ifStatement
    | pair
    ;

// import directive
importStmt  : IMPORT importPath (AS importName)?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

// resource
resource
    :
    resourceType resourceName? NEWLINES
        blockBody
    END
    ;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | string;
blockBody : (blockStatement NEWLINES)*;

blockStatement
    : resource
    | forStatement
    | ifStatement
    | pair
    ;

// virtual resource function
virtualResource      : VR virtualResourceName NEWLINES virtualResourceParam* DEFINE NEWLINES blockBody END;
virtualResourceParam : PARAM IDENTIFIER NEWLINES;
virtualResourceName  : IDENTIFIER;

// forStatement
forVariable : IDENTIFIER;

forStatement
    :
    FOR forVariable (COMMA forVariable)* IN (list | reference) NEWLINES
        blockBody
    END
    ;

// ifStatement
ifStatement
    :
    IF condition NEWLINES
        blockBody
    (ELSEIF condition NEWLINES
        blockBody)*
    (ELSE NEWLINES
        blockBody)?
    END
    ;

comparisonOperator : EQ | NEQ;

condition
    : value                          # ValueCondition
    | value comparisonOperator value # ComparisonCondition
    | condition AND condition        # AndCondition
    | condition OR condition         # OrCondition
    ;

// pair
pair : key COLON value;

key
    : IDENTIFIER
    | STRING
    | IMPORT
    | AS
    | VR
    | PARAM
    | DEFINE
    ;

value
    : booleanValue
    | list
    | map
    | number
    | reference
    | string
    ;

booleanValue
    : TRUE
    | FALSE
    ;

list
    :
    LBRACKET NEWLINES?
        (value (COMMA NEWLINES?
        value)*       NEWLINES?)?
    RBRACKET
    ;

map
    :
    LBRACE NEWLINES?
        (pair (COMMA NEWLINES?
        pair)*       NEWLINES?)?
    RBRACE
    ;

number
    : FLOAT
    | INTEGER
    ;

reference
    : LREF resourceType referenceName (PIPE query)* (PIPE path)? RREF # ResourceReference
    | LREF path RREF                                                  # ValueReference
    ;

referenceName
    : GLOB
    | IDENTIFIER SLASH GLOB
    | resourceName
    ;

query
    : path comparisonOperator value # ComparisonQuery
    | query AND query               # AndQuery
    | query OR query                # OrQuery
    ;

path : IDENTIFIER (DOT IDENTIFIER)*;

string : stringExpression | STRING;
stringExpression : DQUOTE stringContents* DQUOTE;
stringContents : TEXT | reference;
