parser grammar GyroParser;

options { tokenVocab = GyroLexer; }

file
    :
    NEWLINES? statement?
    (NEWLINES statement)*
    NEWLINES? EOF
    ;

statement
    : directive
    | resource
    | virtualResource
    | forStatement
    | ifStatement
    | pair
    ;

// directive
directive : AT IDENTIFIER (value (COMMA? value)*)?;

// resource
resource
    :
    resourceType resourceName? NEWLINES
        blockBody
    END
    ;

resourceType : IDENTIFIER (COLON COLON IDENTIFIER)*;
resourceName : IDENTIFIER | string;
blockBody : (blockStatement NEWLINES)*;

blockStatement
    : directive
    | resource
    | forStatement
    | ifStatement
    | pair
    ;

// virtual resource
virtualResource
    :
    VIRTUAL_RESOURCE resourceType NEWLINES
        (PARAM virtualResourceParameter NEWLINES)*
    DEFINE NEWLINES
        blockBody
    END
    ;

virtualResourceParameter : IDENTIFIER;

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
    (ELSE IF condition NEWLINES
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
    | KEYWORD
    | string
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
    | IDENTIFIER GLOB
    | path
    | string
    ;

query
    : path comparisonOperator value # ComparisonQuery
    | query AND query               # AndQuery
    | query OR query                # OrQuery
    ;

path : IDENTIFIER (DOT IDENTIFIER)*;

string
    : STRING                       # LiteralString
    | DQUOTE stringContent* DQUOTE # InterpolatedString
    ;

stringContent
    : TEXT
    | reference
    ;
