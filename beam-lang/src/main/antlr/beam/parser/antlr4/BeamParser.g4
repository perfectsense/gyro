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
    : blockType param+ tag* (lineSeparator block END)? (lineSeparator | EOF)
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
    : scalar | lineSeparator list | inlineList | lineSeparator map | tagReference
    ;

scalar
    : firstLiteral (restLiteral)*
    ;

firstLiteral
    : literal
    ;

restLiteral
    : DASH | literal
    ;

literal
    : tokenChain | QUOTED_STRING | reference
    ;

tagReference
    : HASH LPAREN TOKEN RPAREN
    ;

reference
    : DOLLAR LPAREN (referenceScope PIPE)* referenceName RPAREN
    ;

tokenChain
    : TOKEN (DOT TOKEN)*
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
    : listEntry+ END
    ;

listEntry
    : DASH scalar lineSeparator
    ;

inlineList
    : LBRACET scalar (COMMA scalar)* RBRACET | RBRACET
    ;

map
    : keyValue+ END
    ;

lineSeparator
    : NEWLINE+
    ;

param
    : literal | inlineList
    ;

tag
    : HASH TOKEN
    ;

