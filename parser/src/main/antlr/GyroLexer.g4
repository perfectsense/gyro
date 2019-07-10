lexer grammar GyroLexer;

// directive
AT    : '@';
COLON : ':';
COMMA : ',';
MINUS : '-';
END   : 'end';

// value
ASTERISK : '*';
SLASH    : '/';
PERCENT  : '%';
PLUS     : '+';
EQ       : '=';
NE       : '!=';
LT       : '<';
LE       : '<=';
GT       : '>';
GE       : '>=';
AND      : 'and';
OR       : 'or';
DOT      : '.';
LPAREN   : '(' -> pushMode(DEFAULT_MODE);
RPAREN   : ')' -> popMode;

// bool
TRUE  : 'true';
FALSE : 'false';

// list
LBRACKET : '[';
RBRACKET : ']';

// map
LBRACE : '{';
RBRACE : '}';

// number
NUMBERS : [0-9] [_0-9]*;

// reference
DOLLAR : '$';
BAR    : '|';

// string
STRING : '\'' ~('\'')* '\'';
DQUOTE : '"' -> pushMode(STRING_MODE);

KEYWORD     : AND | OR;
WHITESPACES : [ \t]+ -> channel(HIDDEN);
NEWLINE     : [\n\r];
COMMENT     : '#' ~[\n\r]* -> channel(HIDDEN);
IDENTIFIER  : [A-Z_a-z] [-/0-9A-Z_a-z]*;

mode STRING_MODE;

S_DOLLAR     : [$\\] -> type(DOLLAR);
S_LPAREN     : '(' -> type(LPAREN), pushMode(DEFAULT_MODE);
S_IDENTIFIER : [A-Z_a-z] [-/0-9A-Z_a-z]* -> type(IDENTIFIER);
S_DQUOTE     : '"' -> type(DQUOTE), popMode;
CHARACTER    : .;
