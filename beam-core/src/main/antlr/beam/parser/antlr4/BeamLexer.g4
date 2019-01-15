lexer grammar BeamLexer;

IMPORT : 'import' -> pushMode(IN_IMPORT);
AS    : 'as';
PROVIDER : 'provider';
STATE : 'state';
FOR   : 'for';
IN    : 'in';
END   : 'end';
TRUE  : 'true';
FALSE : 'false';
IF    : 'if';
ELSEIF: 'else if';
ELSE  : 'else';
EQ    : '==';
NOTEQ : '!=';
OR    : 'or';
AND   : 'and';

COMPARISON_OPERATOR : EQ | NOTEQ;
DECIMAL_LITERAL   : ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
FLOAT_LITERAL     : (Digits '.' Digits? | '.' Digits);
STRING_LITERAL    : '\'' String '\'' ;

QUOTE         : '"' -> pushMode(IN_STRING_EXPRESSION);
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
fragment String : ~('\'')* ;

mode IN_STRING_EXPRESSION;

S_QUOTE      : '"' -> type(QUOTE), popMode;
S_DOLLAR     : '$' -> type(DOLLAR);
S_DOLLAR_L   : '$(' -> skip, pushMode(IN_REFERENCE);
S_LPAREN     : '(' -> type(LPAREN);
S_RPAREN     : ')' -> type(RPAREN);
TEXT         : ~[$("]+;

mode IN_REFERENCE;

R_DOT        : '.' -> type(DOT);
R_DOLLAR     : '$' -> type(DOLLAR);
R_PIPE       : '|' -> type(PIPE);
R_LPAREN     : '(' -> type(LPAREN);
R_RPAREN     : ')' -> skip, popMode;
R_QUOTE      : '"' -> type(QUOTE), pushMode(IN_STRING_EXPRESSION);
R_IDENTIFIER : (Common | COLON COLON)+ -> type(IDENTIFIER);
R_WS         : [ \u000C\t\r\n]+ -> channel(HIDDEN);

mode IN_IMPORT;

I_AS         : 'as' -> type(AS);
I_DOT        : '.' -> type(DOT);
I_SLASH      : '/' -> type(SLASH);
I_IDENTIFIER : (SLASH | DOT | Common)+ -> type(IDENTIFIER);
I_NEWLINE    : [\r\n] -> skip, popMode;
I_WS         : [ \u000C\t]+ -> channel(HIDDEN);
