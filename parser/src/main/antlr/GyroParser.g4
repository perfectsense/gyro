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
    | pair
    ;

// directive
directive
    : AT IDENTIFIER COLON arguments
    | AT IDENTIFIER arguments? option* NEWLINES body section* AT END
    ;

arguments : value (COMMA? value)*;

option: TILDE IDENTIFIER arguments?;

body : (statement NEWLINES)*;

section: option NEWLINES body;

// block
block
    : IDENTIFIER name? NEWLINES body END # KeyBlock
    | type name NEWLINES body END        # Resource
    ;

type : IDENTIFIER COLON COLON IDENTIFIER;

name
    : IDENTIFIER
    | reference
    | string
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
    : IDENTIFIER
    | ASTERISK
    | NUMBERS
    | string
    ;

item
    : bool
    | list
    | map
    | number
    | reference
    | string
    | type
    | word
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

number : MINUS? NUMBERS (DOT NUMBERS)?;

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

word
    : IDENTIFIER ASTERISK?
    | ASTERISK
    ;
