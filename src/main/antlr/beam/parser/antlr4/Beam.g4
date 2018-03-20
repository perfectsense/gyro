grammar Beam;

@header {
    package beam.parser.antlr4;
}

/*
 * Parser Rules
 */

beamRoot
    : globalScope EOF
    ;

globalScope
    : (providerBlock | method | providerImport)+
    ;

resourceScope
    :  (providerBlock | method | keyValueBlock)+
    ;

providerBlock
    : (resourceSymbol SYMBOL_ASSIGN)? providerName LBLOCK (resourceScope)* RBLOCK
    ;

providerName
    : PROVIDER_NAME
    ;

providerImport
    : PROVIDER_IMPORT providerLocation
    ;

providerLocation
    : QUOTED_STRING
    ;

map
    : LBLOCK (keyValueBlock)* RBLOCK
    ;

list
    : '['value? (',' value)* ']'
    ;

key
    : ID COLON
    ;

value
    : QUOTED_STRING | ID | NUMBERS | list | map
    ;

keyValueBlock
    : key value
    ;

method
    : (
        (METHOD methodArguments (COMMA methodArguments)*) |
        (METHOD LPAREN methodArguments (COMMA methodArguments)* RPAREN)
      )+
    ;

methodArguments
    : (QUOTED_STRING | methodNamedArgument)+
    ;

methodNamedArgument
    : ID MAP_ASSIGN QUOTED_STRING
    ;

resourceSymbol
    : ID
    ;
/*
 * Lexer Rules
 */

fragment LETTER : [a-zA-Z_.] ;
fragment DIGIT  : [0-9] ;

PROVIDER_IMPORT
    : 'provider' WHITESPACE
    ;

QUOTED_STRING
    : '"' ~('\\'|'"')* '"'
    | '\'' ~('\''|'"')* '\''
    ;

NUMBERS
    : DIGIT+
    ;

ID
    : LETTER (LETTER|'0'..'9'|' ')*
    ;

PROVIDER_NAME
    : ID '::' ID ;

INT
    : DIGIT+ ;

// Keywords
MODULE_INCLUDE: 'module' ;

HASH          : '#' ;
LPAREN        : '(' ;
RPAREN        : ')' ;
LBLOCK        : '{' ;
RBLOCK        : '}' ;
METHOD        : '@' ID ;
MAP_ASSIGN    : '=>' ;
SYMBOL_ASSIGN : '=' ;
COMMA         : ',' ;
COLON         : ':' ;

// Whitespace

NEWLINE       : [\n\r]+ -> channel(HIDDEN);
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT       : '#' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;
