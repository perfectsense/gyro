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
    | block
    | virtualResource
    | forStatement
    | ifStatement
    | pair
    ;

// directive
directive : AT IDENTIFIER (directiveArgument (COMMA? directiveArgument)*)?;

directiveArgument
    : block
    | value
    ;

// block
block
    : IDENTIFIER  NEWLINES blockBody END # KeyBlock
    | type string NEWLINES blockBody END # Resource
    ;

type : IDENTIFIER (COLON COLON IDENTIFIER)?;
blockBody : (blockStatement NEWLINES)*;

blockStatement
    : directive
    | block
    | forStatement
    | ifStatement
    | pair
    ;

// virtual resource
virtualResource
    :
    VIRTUAL_RESOURCE type NEWLINES
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

reference : LREF value* (PIPE query)* RREF;

query
    : path comparisonOperator value # ComparisonQuery
    | query AND query               # AndQuery
    | query OR query                # OrQuery
    ;

path : IDENTIFIER (DOT IDENTIFIER)*;

string
    : STRING                       # LiteralString
    | DQUOTE stringContent* DQUOTE # InterpolatedString
    |
        ( IDENTIFIER GLOB
        | IDENTIFIER
        | GLOB
        | type
    )                              # BareString
    ;

stringContent
    : TEXT
    | reference
    ;
