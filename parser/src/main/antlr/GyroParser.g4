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
    | forStatement
    | ifStatement
    | pair
    ;

// directive
directive
    : AT IDENTIFIER COLON arguments
    | AT IDENTIFIER arguments? option* NEWLINES blockBody section* AT END;

arguments : value (COMMA? value)*;

option: TILDE IDENTIFIER arguments?;

section: option NEWLINES blockBody;

// block
block
    : IDENTIFIER name? NEWLINES blockBody END # KeyBlock
    | type name NEWLINES blockBody END        # Resource
    ;

type : IDENTIFIER COLON COLON IDENTIFIER;

name
    : reference
    | string
    ;

blockBody : (blockStatement NEWLINES)*;

blockStatement
    : directive
    | block
    | forStatement
    | ifStatement
    | pair
    ;

// forStatement
forStatement
    :
    FOR forVariable (COMMA forVariable)* IN forValue NEWLINES
        blockBody
    END
    ;

forVariable : IDENTIFIER;

forValue
    : list
    | map
    | reference
    ;

// ifStatement
ifStatement
    :
    IF value NEWLINES
        blockBody
    (ELSE IF value NEWLINES
        blockBody)*
    (ELSE NEWLINES
        blockBody)?
    END
    ;

// pair
pair : key COLON value;

key
    : IDENTIFIER
    | KEYWORD
    | string
    ;

value
    : and          # OneValue
    | and OR value # TwoValue
    ;

and
    : rel         # OneAnd
    | rel AND and # TwoAnd
    ;

rel
    : add           # OneRel
    | add relOp rel # TwoRel
    ;

relOp
    : EQ
    | NE
    | LT
    | LE
    | GT
    | GE
    ;

add
    : mul           # OneAdd
    | mul addOp add # TwoAdd
    ;

addOp
    : PLUS
    | MINUS
    ;

mul
    : mulItem           # OneMul
    | mulItem mulOp mul # TwoMul
    ;

mulOp
    : ASTERISK
    | SLASH
    | PERCENT
    ;

mulItem
    : item                # OneMulItem
    | item (DOT index)+   # IndexedMulItem
    | LPAREN value RPAREN # GroupedMulItem
    ;

index
    : string
    | number
    ;

item
    : bool
    | list
    | map
    | number
    | reference
    | string
    ;

bool
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
    : DOLLAR LPAREN value* (PIPE filter)* RPAREN
    | DOLLAR IDENTIFIER
    ;

filter
    : IDENTIFIER relOp value # ComparisonFilter
    | filter AND filter      # AndFilter
    | filter OR filter       # OrFilter
    ;

string
    : STRING                       # LiteralString
    | DQUOTE stringContent* DQUOTE # InterpolatedString
    |
        ( IDENTIFIER ASTERISK
        | IDENTIFIER
        | ASTERISK
        | type
    )                              # BareString
    ;

stringContent
    : reference
    | text
    ;

text
    : DOLLAR
    | LPAREN
    | (IDENTIFIER | CHARACTER)+
    ;
