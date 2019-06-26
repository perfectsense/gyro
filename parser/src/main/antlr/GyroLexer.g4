lexer grammar GyroLexer;

// directive
AT    : '@';

// resource
END : 'end';

// forStatement
FOR   : 'for';
COMMA : ',';
IN    : 'in';

// ifStatement
IF   : 'if';
ELSE : 'else';
EQ   : '=';
NEQ  : '!=';
AND  : 'and';
OR   : 'or';

// pair
COLON : ':';

// booleanValue
TRUE  : 'true';
FALSE : 'false';

// list
LBRACKET : '[';
RBRACKET : ']';

// map
LBRACE : '{';
RBRACE : '}';

// number
FLOAT   : '-'? [0-9] [_0-9]* '.' [_0-9]*;
INTEGER : '-'? [0-9] [_0-9]*;

// reference
DOLLAR : '$';
LREF   : DOLLAR '(' -> pushMode(DEFAULT_MODE);
RREF   : ')' -> popMode;
GLOB   : '*';
PIPE   : '|';
DOT    : '.';

// string
STRING : '\'' ~('\'')* '\'';
DQUOTE : '"' -> pushMode(STRING_MODE);

KEYWORD     : IN | AND | OR;
WHITESPACES : [ \t]+ -> channel(HIDDEN);
COMMENT     : '#' ~[\n\r]* NEWLINES -> channel(HIDDEN);
NEWLINES    : [\n\r][ \t\n\r]*;
IDENTIFIER  : [A-Z_a-z] [-/0-9A-Z_a-z]*;

mode STRING_MODE;

S_LREF       : [\\$] '(' -> type(LREF), pushMode(DEFAULT_MODE);
S_DOLLAR     : '$' -> type(DOLLAR);
S_IDENTIFIER : [A-Z_a-z] [-/0-9A-Z_a-z]* -> type(IDENTIFIER);
S_DQUOTE     : '"' -> type(DQUOTE), popMode;
CHARACTER    : .;
