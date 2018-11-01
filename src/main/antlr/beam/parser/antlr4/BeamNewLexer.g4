lexer grammar BeamNewLexer;

/*
 * Lexer Rules
 */

fragment DIGIT  : [0-9] ;
fragment LETTER : [A-Za-z] ;
fragment UNDER_SCORE : [_] ;
fragment STRING : ~('\''|'"')* ;

END
    : 'end'
    ;

DONE
    : 'done'
    ;

LITERAL
    : '"' STRING '"' | '\'' STRING '\''
    ;

COLON
    : ':'
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
PIPE          : '|' ;
DOT           : '.' ;
SLASH         : '/' ;
STAR          : '*' ;
DASH          : '-' ;

TOKEN
    : (LETTER | DIGIT | DASH | UNDER_SCORE | DOT | SLASH | COMMA | LBLOCK | RBLOCK | LBRACET | RBRACET | STAR)+
    ;

// Whitespace
WS            : [ \t]+ -> skip ;
NEWLINE       : [\r\n] ;
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN) ;
COMMENT       : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;
