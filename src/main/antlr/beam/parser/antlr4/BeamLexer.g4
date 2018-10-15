lexer grammar BeamLexer;

/*
 * Lexer Rules
 */

// key words
IMPORT
    : 'import'
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
END           : 'end' ;

fragment LETTER : [a-zA-Z_.] ;
fragment DIGIT  : [0-9] ;
fragment STRING : ~('\''|'"')* ;
fragment UPPER     : [A-Z_];
fragment LOWER     : [a-z];


CONST_VAR
    : UPPER+
    ;

NUMBERS
    : '-'?DIGIT+
    ;

ID
    : LOWER (LOWER | DIGIT | DASH)*
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
DOLLAR        : '$' ;
AT            : '@' ;
COMMA         : ',' ;
DOT           : '.' ;
PIPE          : '|' ;
ASSIGN        : '=' -> pushMode(SCALAR) ;
COLON         : ':'-> pushMode(SCALAR) ;
DASH          : '-' -> pushMode(SCALAR) ;
STAR          : '*'  ;

// Whitespace
WS            : [ \t]+ -> skip;
NEWLINE       : [\r\n] ;
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT       : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;

mode SCALAR;
SCALAR_HASH          : '#' -> type(HASH), pushMode(REFERENCE) ;
SCALAR_DOLLAR        : '$' -> type(DOLLAR), pushMode(REFERENCE) ;
SCALAR_AT            : '@' -> type(AT), pushMode(REFERENCE) ;
SCALAR_WS            : [ \t] -> type(WS);

UNQUOTED_LITERAL : ~('\''|'"'|'\n'|'\r'|' '|'\t'|'@'|'$'|'#')+;
SCALAR_END: [\r\n] -> type(NEWLINE), popMode;

mode REFERENCE;
REFERENCE_HASH          : '#' -> type(HASH) ;
REFERENCE_DOLLAR        : '$' -> type(DOLLAR) ;
REFERENCE_AT            : '@' -> type(AT) ;

REFERENCE_CONST_VAR
    : UPPER+ -> type(CONST_VAR)
    ;

REFERENCE_ID
    : LOWER (LOWER | DIGIT | DASH)* -> type(ID)
    ;

REFERENCE_RESOURCE_PROVIDER
    : REFERENCE_ID '::' REFERENCE_ID -> type(RESOURCE_PROVIDER) ;

REFERENCE_LPAREN        : '(' -> type(LPAREN) ;
REFERENCE_RPAREN        : ')' -> type(RPAREN), popMode ;
REFERENCE_DOT           : '.' -> type(DOT) ;
REFERENCE_PIPE          : '|' -> type(PIPE) ;
REFERENCE_SKIP  : [ \t]+ -> skip;