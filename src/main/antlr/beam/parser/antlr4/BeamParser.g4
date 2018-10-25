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
    : pluginBlock* importBlock* assignmentBlock* resourceBlock*
    ;

pluginBlock
    : PLUGIN path lineSeparator
    ;

importBlock
    : IMPORT path AS ID lineSeparator
    ;

path
    : QUOTED_STRING
    ;

assignmentBlock
    : CONST_VAR ASSIGN value
    ;

resourceBlock
    : RESOURCE_PROVIDER ID lineSeparator resourceBody END (lineSeparator | EOF)
    ;

lineSeparator
    : NEWLINE+
    ;

resourceBody
    : (keyValueBlock | actionBlock)*
    ;

keyValueBlock
    : key COLON value
    ;

key
    : ID
    ;

value
    : scalar lineSeparator | WS* NEWLINE list NEWLINE* | WS* NEWLINE map NEWLINE*
    ;

scalar
    : scalarFirstLiteral scalarRestLiterals
    ;

scalarFirstLiteral
    : scalarLiteral
    ;

scalarRestLiterals
    : (scalarLiteral)*
    ;

scalarLiteral
    : WS* (reference | unquotedLiteral)
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
    : (keyValueBlock)*
    ;

reference
    : resourceReference | tagReference | constantReference
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
    : (ID.)* VARIABLE
    ;
