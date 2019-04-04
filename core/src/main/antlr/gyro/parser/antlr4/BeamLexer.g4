lexer grammar BeamLexer;

DEFINE : 'define';
VR    : 'virtual-resource';
PARAM : 'param';
FOR   : 'for';
IN    : 'in';
END   : 'end';
TRUE  : 'true';
FALSE : 'false';
IF    : 'if';
ELSEIF: 'else if';
ELSE  : 'else';
EQ    : '=';
NEQ : '!=';
OR    : 'or';
AND   : 'and';

FLOAT   : '-'? (Digits '.' Digits? | '.' Digits);
INTEGER : '-'? ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
STRING  : '\'' String '\'' ;

AT            : '@';
DQUOTE         : '"' -> pushMode(IN_STRING_EXPRESSION);
COLON         : ':';
HASH          : '#';
LBRACE        : '{';
RBRACE        : '}';
LBRACKET      : '[';
RBRACKET      : ']';
LREF          : '$(' -> pushMode(DEFAULT_MODE);
RREF          : ')' -> popMode;
COMMA         : ',';
PIPE          : '|';
SLASH         : '/';
DOT           : '.';
GLOB          : '*';

IDENTIFIER : (Common | COLON COLON)+;

NEWLINES : [\r\n]+[ \u000C\t\r\n]*;
WS      : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT : HASH ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;

fragment Digits : [0-9] ([0-9_]* [0-9])?;
fragment Letter : [A-Za-z] ;
fragment LetterOrDigits : Letter | [0-9];
fragment Common : LetterOrDigits | '_' | '-';
fragment String : ~('\'')* ;

mode IN_STRING_EXPRESSION;

TEXT     : ~[$("]+;
S_LREF   : '$(' -> type(LREF), pushMode(DEFAULT_MODE);
S_DOLLAR : '$' -> type(TEXT);
S_LPAREN : '(' -> type(TEXT);
S_DQUOTE : '"' -> type(DQUOTE), popMode;