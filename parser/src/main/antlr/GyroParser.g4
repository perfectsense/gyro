parser grammar GyroParser;

options { tokenVocab = GyroLexer; }

file
    :
    NEWLINE*  statement?
    (NEWLINE+ statement)*
    NEWLINE*  EOF
    ;

statement
    : directive
    | block
    | pair
    ;

// directive
directive
    : AT directiveType COLON arguments option*
    | AT directiveType arguments? option* NEWLINE+ body section* AT END
    ;

directiveType : IDENTIFIER (COLON COLON IDENTIFIER)?;

arguments : value (COMMA? value)*;

option: MINUS IDENTIFIER arguments?;

body : (statement NEWLINE+)*;

section: option NEWLINE+ body;

// block
block
    : IDENTIFIER name? NEWLINE+ body END # KeyBlock
    | type name NEWLINE+ body END        # Resource
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
    LBRACKET NEWLINE*
        (value (COMMA NEWLINE*
        value)*       NEWLINE*)?
    RBRACKET
    ;

map
    :
    LBRACE NEWLINE*
        (pair (COMMA NEWLINE*
        pair)*       NEWLINE*)?
    RBRACE
    ;

number : MINUS? NUMBERS (DOT NUMBERS)?;

reference
    : DOLLAR LPAREN value* (BAR filter)* RPAREN
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
