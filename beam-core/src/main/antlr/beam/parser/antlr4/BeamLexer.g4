lexer grammar BeamLexer;

FOR   : 'for';
IN    : 'in';
END   : 'end';
TRUE  : 'true';
FALSE : 'false';

DECIMAL_LITERAL   : ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
FLOAT_LITERAL     : (Digits '.' Digits? | '.' Digits);
STRING_INTEPRETED : '"' String '"';
STRING_LITERAL    : '\'' String '\'' ;

COLON         : ':';
HASH          : '#';
LCURLY        : '{';
RCURLY        : '}';
LBRACKET      : '[';
RBRACKET      : ']';
DOLLAR        : '$';
COMMA         : ',';
LPAREN        : '(';
RPAREN        : ')';
PIPE          : '|';
SLASH         : '/';
DOT           : '.';

IDENTIFIER : (Common | COLON COLON)+;

WS      : [ \u000C\t\r\n]+ -> channel(HIDDEN);
COMMENT : HASH ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;

fragment Digits : [0-9] ([0-9_]* [0-9])?;
fragment Letter : [A-Za-z] ;
fragment LetterOrDigits : Letter | [0-9];
fragment Common : LetterOrDigits | '_' | '-';
fragment String : ~('\''|'"')* ;
