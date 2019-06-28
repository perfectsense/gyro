lexer grammar GyroLexer;

// directive
AT    : '@';
COLON : ':';
COMMA : ',';
TILDE : '~';
END   : 'end';

// value
ASTERISK : '*';
SLASH    : '/';
PERCENT  : '%';
PLUS     : '+';
MINUS    : '-';
EQ       : '=';
NE       : '!=';
LT       : '<';
LE       : '<=';
GT       : '>';
GE       : '>=';
AND      : 'and';
OR       : 'or';
LPAREN   : '(' -> pushMode(DEFAULT_MODE);
RPAREN   : ')' -> popMode;

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
DOT     : '.';
NUMBERS : [0-9] [_0-9]*;

// reference
DOLLAR : '$';
PIPE   : '|';

// string
STRING : '\'' ~('\'')* '\'';
DQUOTE : '"' -> pushMode(STRING_MODE);

KEYWORD     : AND | OR;
WHITESPACES : [ \t]+ -> channel(HIDDEN);
COMMENT     : '#' ~[\n\r]* NEWLINES -> channel(HIDDEN);
NEWLINES    : [\n\r] [ \t\n\r]*;
IDENTIFIER  : [A-Z_a-z] [-/0-9A-Z_a-z]*;

mode STRING_MODE;

S_DOLLAR     : [$\\] -> type(DOLLAR);
S_LPAREN     : '(' -> type(LPAREN), pushMode(DEFAULT_MODE);
S_IDENTIFIER : [A-Z_a-z] [-/0-9A-Z_a-z]* -> type(IDENTIFIER);
S_DQUOTE     : '"' -> type(DQUOTE), popMode;
CHARACTER    : .;
