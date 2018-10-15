parser grammar BeamParser;

options
   { tokenVocab = BeamLexer; }
/*
 * Parser Rules
 */

beamRoot
    : globalScope EOF
    ;

globalScope
    : (resourceBlock)+
    ;

resourceBlock
    : RESOURCE_PROVIDER ID lineSeparator resourceBody END lineSeparator
    ;

lineSeparator
    : NEWLINE+
    ;

resourceBody
    : (keyValueBlock lineSeparator | actionBlock)*
    ;

keyValueBlock
    : key COLON value
    ;

key
    : ID
    ;

value
    : scalar | NEWLINE list | NEWLINE map
    ;

scalar
    : scalarFirstLiteral scalarRestLiterals
    ;

scalarFirstLiteral
    : SCALAR_WS* (reference | unquotedLiteral)
    ;

scalarRestLiterals
    : (SCALAR_WS* (reference | unquotedLiteral))*
    ;

list
    : (listEntry)+
    ;

listEntry
    : DASH scalar NEWLINE
    ;

map
    : (mapEntry)+
    ;

mapEntry
    : STAR keyScalarBlock NEWLINE
    ;

keyScalarBlock
    : key COLON scalar
    ;

actionBlock
    : ID lineSeparator actionBody END lineSeparator
    ;

actionBody
    : (keyValueBlock lineSeparator)*
    ;

reference
    : resourceReference | tagReference | constantReference
    ;

unquotedString
    : unquotedLiteral (SCALAR_WS* (unquotedLiteral | reference))*
    ;

unquotedLiteral
    : UNQUOTED_LITERAL
    ;

resourceReference
    : AT LPAREN RESOURCE_PROVIDER referenceChain PIPE referenceChain RPAREN
    ;

tagReference
    : HASH LPAREN RESOURCE_PROVIDER referenceChain PIPE referenceChain RPAREN
    ;

referenceChain
    : ID (DOT ID)*
    ;

constantReference
    : DOLLAR LPAREN constantReferenceChain RPAREN
    ;

constantReferenceChain
    : (ID.)* CONST_VAR
    ;
