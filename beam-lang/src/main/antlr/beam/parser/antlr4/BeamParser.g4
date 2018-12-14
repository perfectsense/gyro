parser grammar BeamParser;

options
   { tokenVocab = BeamLexer; }

/*
 * Parser Rules
 */

beamRoot
    : NEWLINE* block* EOF
    ;

block
    :  (blockBody | keyValue)+
    ;

blockBody
    : blockType parameter+ (lineSeparator block END)? (lineSeparator | EOF)
    ;

blockType
    : TOKEN
    ;

keyValue
    : key COLON value (lineSeparator | EOF)
    ;

key
    : TOKEN | QUOTED_STRING
    ;

value
    : scalar | list | map
    ;

scalar
    : QUOTED_STRING
    | unquotedString
    | reference
    ;

unquotedString
    : TOKEN (WHITESPACE* TOKEN)*
    ;

tokenChain
    : TOKEN (DOT TOKEN)*
    ;

reference
    : DOLLAR LPAREN (referenceScope PIPE)* referenceName RPAREN
    ;

referenceScope
    : referenceType? referenceId
    ;

referenceName
    : referenceChain | referenceScope
    ;

referenceType
    : TOKEN
    ;

referenceId
    : TOKEN
    ;

referenceChain
    : tokenChain
    ;

list
    : (LBRACET | LBRACET lineSeparator) scalar ((COMMA | lineSeparator) scalar)* (lineSeparator RBRACET | RBRACET)
    ;

map
    : (LBLOCK lineSeparator | LBLOCK) mapKeyValue+ (lineSeparator RBLOCK | RBLOCK)
    ;

mapKeyValue
    : key COLON value (lineSeparator | EOF)
    ;

parameter
    : scalar | list
    ;

lineSeparator
    : NEWLINE+
    ;
