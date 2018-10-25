lexer grammar BeamLexer;

/*
 * Lexer Rules
 */

IMPORT
    : 'import'
    ;

PLUGIN
    : 'plugin'
    ;

AS
    : 'as'
    ;

END
    : 'end'
    ;

fragment DIGIT  : [0-9] ;
fragment STRING : ~('\''|'"')* ;
fragment UPPER_CASE : [A-Z] ;
fragment LOWER_CASE : [a-z] ;
fragment UNDER_SCORE : [_] ;

VARIABLE
    : UPPER_CASE (UPPER_CASE | UNDER_SCORE)*
    ;

ID
    : LOWER_CASE (LOWER_CASE | DIGIT | DASH)*
    ;

RESOURCE_PROVIDER
    : ID '::' ID ;

INT
    : DIGIT+ ;

QUOTED_STRING
    : '"' STRING '"'
    | '\'' STRING '\''
    ;

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
COLON         : ':' -> pushMode(SCALAR) ;
DASH          : '-' -> pushMode(SCALAR) ;
STAR          : '*' -> pushMode(MAP) ;

// Whitespace
WS            : [ \t]+ -> skip;
NEWLINE       : [\r\n] ;
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT       : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;

mode MAP ;

MAP_KEY
    : (LOWER_CASE | UPPER_CASE) (LOWER_CASE | UPPER_CASE | DIGIT | DASH | UNDER_SCORE)*
    ;

MAP_COLON
    : ':' -> type(COLON), popMode, pushMode(SCALAR)
    ;

MAP_SKIP
    : [ \t]+ -> skip
    ;

mode SCALAR ;

SCALAR_HASH
    : '#' -> type(HASH), pushMode(REFERENCE)
    ;

SCALAR_DOLLAR
    : '$' -> type(DOLLAR), pushMode(REFERENCE)
    ;

SCALAR_AT
    : '@' -> type(AT), pushMode(REFERENCE)
    ;

SCALAR_WS
    : [ \t] -> type(WS)
    ;

UNQUOTED_LITERAL
    : ~('\''|'"'|'\n'|'\r'|' '|'\t'|'@'|'$'|'#')+
    ;

SCALAR_END
    : [\r\n] -> type(NEWLINE), popMode
    ;

mode REFERENCE ;

REFERENCE_HASH
    : '#' -> type(HASH)
    ;

REFERENCE_DOLLAR
    : '$' -> type(DOLLAR)
    ;

REFERENCE_AT
    : '@' -> type(AT)
    ;

REFERENCE_VAR
    : UPPER_CASE (UPPER_CASE | UNDER_SCORE)* -> type(VARIABLE)
    ;

REFERENCE_ID
    : LOWER_CASE (LOWER_CASE | DIGIT | DASH)* -> type(ID)
    ;

REFERENCE_PROVIDER
    : REFERENCE_ID '::' REFERENCE_ID -> type(RESOURCE_PROVIDER)
    ;

REFERENCE_LPAREN
    : '(' -> type(LPAREN)
    ;

REFERENCE_RPAREN
    : ')' -> type(RPAREN), popMode
    ;

REFERENCE_DOT
    : '.' -> type(DOT)
    ;

REFERENCE_PIPE
    : '|' -> type(PIPE)
    ;

REFERENCE_SKIP
    : [ \t]+ -> skip
    ;
