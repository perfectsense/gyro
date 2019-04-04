lexer grammar BeamLexer;

// directive
AT    : '@';
DOT   : '.';
SLASH : '/';

// resource
END : 'end';

// virtual resource
VIRTUAL_RESOURCE : 'virtual-resource';
PARAM            : 'param';
DEFINE           : 'define';

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
FLOAT   : '-'? ([0-9] [_0-9]* '.' [_0-9]* | '.' [_0-9]+);
INTEGER : '-'? [0-9] [_0-9]*;

// reference
LREF : '$(' -> pushMode(DEFAULT_MODE);
RREF : ')' -> popMode;
GLOB : '*';
PIPE : '|';

// string
STRING : '\'' ~('\'')* '\'';
DQUOTE : '"' -> pushMode(STRING_MODE);

KEY         : PARAM | DEFINE | IN | AND | OR;
WHITESPACES : [ \t]+ -> channel(HIDDEN);
COMMENT     : '#' ~[\n\r]* ('\n' | '\r' | '\r\n') -> channel(HIDDEN);
NEWLINES    : [\n\r][ \t\n\r]*;
IDENTIFIER  : [_A-Za-z] [-_0-9A-Za-z]*;

mode STRING_MODE;

TEXT     : ~[$("]+;
S_LREF   : '$(' -> type(LREF), pushMode(DEFAULT_MODE);
S_DOLLAR : '$' -> type(TEXT);
S_LPAREN : '(' -> type(TEXT);
S_DQUOTE : '"' -> type(DQUOTE), popMode;