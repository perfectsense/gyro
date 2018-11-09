parser grammar BeamParser;

options
   { tokenVocab = BeamLexer; }
/*
 * Parser Rules
 */

beamRoot
    : NEWLINE* globalScope EOF
    ;

globalScope
    : (keyValuePair | extension)*
    ;

keyValuePair
    : key COLON value (lineSeparator | EOF)
    ;

key
    : TOKEN | QUOTED_STRING
    ;

value
    : scalar | lineSeparator list | inlineList | lineSeparator map
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
    : keyValuePair+ END
    ;

lineSeparator
    : NEWLINE+
    ;

extension
    : extensionName param+ (lineSeparator methodBody)? (lineSeparator | EOF)
    ;

extensionName
    : TOKEN
    ;

param
    : literal | inlineList
    ;

methodBody
    :  (extension | keyValuePair)+ END
    ;
