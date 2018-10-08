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
    : ( pluginBlock | importBlock | resourceBlock | assignmentBlock)+
    ;

pluginBlock
    : PLUGIN path
    ;

importBlock
    : IMPORT path AS variable
    ;

resourceBlock
    : resourceProvider variable map
    ;

assignmentBlock
    : LET variable ASSIGN value
    ;

resourceProvider
    : RESOURCE_PROVIDER
    ;

map
    : LBLOCK (keyValueBlock | resourceBlock)* RBLOCK
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

reference
    : REFERENCE LBLOCK referenceChain RBLOCK
    ;

referenceChain
    : ID (DOT ID)*
    ;

variable
    : ID
    ;

path
    : QUOTED_STRING
    ;

/*
 * Lexer Rules
 */

// key words
IMPORT
    : 'import'
    ;

LET
    : 'let'
    ;

PLUGIN
    : 'plugin'
    ;

WORKFLOW
    : 'workflow'
    ;

AS
    : 'as'
    ;

fragment LETTER : [a-zA-Z_.] ;
fragment DIGIT  : [0-9] ;

QUOTED_STRING
    : '"' ~('\\'|'"')* '"'
    | '\'' ~('\''|'"')* '\''
    ;

NUMBERS
    : '-'?DIGIT+
    ;

ID
    : LETTER (LETTER | DIGIT)*
    ;

RESOURCE_PROVIDER
    : ID '::' ID ;

INT
    : DIGIT+ ;

HASH          : '#' ;
LPAREN        : '(' ;
RPAREN        : ')' ;
LBLOCK        : '{' ;
RBLOCK        : '}' ;
LBRACET       : '[' ;
RBRACET       : ']' ;
ASSIGN        : '=' ;
COLON         : ':' ;
REFERENCE     : '$' ;
COMMA         : ',' ;
DOT           : '.' ;

// Whitespace
WS            : [ \t\n\r]+ -> skip;
NEWLINE       : [\n\r]+ -> channel(HIDDEN);
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT       : '#' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;