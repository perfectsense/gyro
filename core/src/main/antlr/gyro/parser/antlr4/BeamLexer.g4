lexer grammar BeamLexer;

IMPORT : 'import' -> pushMode(IN_IMPORT);
DEFINE : 'define';
VR    : 'virtual-resource';
PARAM : 'param';
AS    : 'as';
FOR   : 'for';
IN    : 'in';
END   : 'end';
TRUE  : 'true';
FALSE : 'false';
IF    : 'if';
ELSEIF: 'else if';
ELSE  : 'else';
EQ    : '=';
NOTEQ : '!=';
OR    : 'or';
AND   : 'and';

COMPARISON_OPERATOR : EQ | NOTEQ;
DECIMAL_LITERAL   : '-'? ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
FLOAT_LITERAL     : '-'? (Digits '.' Digits? | '.' Digits);
STRING_LITERAL    : '\'' String '\'' ;

DQUOTE         : '"' -> pushMode(IN_STRING_EXPRESSION);
COLON         : ':';
HASH          : '#';
LCURLY        : '{';
RCURLY        : '}';
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

NEWLINE : [\r\n]+[ \u000C\t\r\n]*;
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

mode IN_IMPORT;

I_AS         : 'as' -> type(AS);
I_DOT        : '.' -> type(DOT);
I_SLASH      : '/' -> type(SLASH);
I_IDENTIFIER : (SLASH | DOT | Common)+ -> type(IDENTIFIER);
I_NEWLINE    : [\r\n] -> skip, popMode;
I_WS         : [ \u000C\t]+ -> channel(HIDDEN);
