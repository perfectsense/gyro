grammar Beam;

@header {
    package beam.parser.antlr4;
}

/*
 * Parser Rules
 */

beamRoot
    : globalScope EOF
    ;

globalScope
    : (providerBlock | providerImport | assignmentBlock | includeStatement)+
    ;

includeStatement
    : INCLUDE QUOTED_STRING AS alias
    ;

resourceScope
    : (providerBlock | keyValueBlock)+
    ;

providerBlock
    : providerName resourceSymbol map
    ;

providerName
    : PROVIDER_NAME
    ;

providerImport
    : PROVIDER_IMPORT providerLocation
    ;

providerLocation
    : QUOTED_STRING
    ;

map
    : LBLOCK (keyValueBlock)* RBLOCK
    ;

list
    : (LBRACET value? (COMMA value)* RBRACET)
    ;

key
    : ID
    ;

value
    : QUOTED_STRING | ID | NUMBERS | list | map | reference
    ;

keyValueBlock
    : key COLON value
    ;

assignmentBlock
    : VARIABLE resourceSymbol SYMBOL_ASSIGN value
    ;

reference
    : REFER LBLOCK ID (DOT ID)* RBLOCK
    ;

resourceSymbol
    : ID
    ;

alias
    : ID
    ;

/*
 * Lexer Rules
 */

fragment LETTER : [a-zA-Z_.] ;
fragment DIGIT  : [0-9] ;

VARIABLE
    : 'let'
    ;

REFER
    : '$'
    ;

PROVIDER_IMPORT
    : 'provider'
    ;

INCLUDE
    : 'include'
    ;

AS
    : 'as'
    ;

QUOTED_STRING
    : '"' ~('\\'|'"')* '"'
    | '\'' ~('\''|'"')* '\''
    ;

NUMBERS
    : DIGIT+
    ;

ID
    : LETTER (LETTER | DIGIT)*
    ;

COMMA
    : ','
    ;

DOT
    : '.'
    ;

PROVIDER_NAME
    : ID '::' ID ;

INT
    : DIGIT+ ;

// Keywords
HASH          : '#' ;
LPAREN        : '(' ;
RPAREN        : ')' ;
LBLOCK        : '{' ;
RBLOCK        : '}' ;
LBRACET       : '[' ;
RBRACET       : ']' ;
METHOD        : '@' ID ;
MAP_ASSIGN    : '=>' ;
SYMBOL_ASSIGN : '=' ;
COLON         : ':' ;

// Whitespace
WS            : [ \t\n\r]+ -> skip;
NEWLINE       : [\n\r]+ -> channel(HIDDEN);
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT       : '#' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;