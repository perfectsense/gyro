parser grammar BeamNewParser;

options
   { tokenVocab = BeamNewLexer; }
/*
 * Parser Rules
 */

beamRoot
    : NEWLINE* globalScope EOF
    ;

globalScope
    : config*
    ;

config
    : extension? param+ (configBlock)? (lineSeparator | EOF)
    ;

lineSeparator
    : NEWLINE+
    ;

extension
    : TOKEN
    ;

param
    : literal | inlineList
    ;

literal
    : (TOKEN | QUOTED_STRING | reference)
    ;

configBlock
    : completeBlock | simpleBlock
    ;

completeBlock
    : COLON lineSeparator (config* | listEntry*) END
    ;

listEntry
    : listDelineator value lineSeparator
    ;

listDelineator
    : DASH
    ;

simpleBlock
    : COLON (inlineList | value)
    ;

value
    : literal (DASH | literal)*
    ;

inlineList
    : LBRACET value (COMMA value)* RBRACET | RBRACET
    ;

reference
    : resourceReference
    ;

resourceReference
    : AT LPAREN referenceType? referenceName (PIPE referenceChain)? RPAREN
    ;

referenceType
    : TOKEN
    ;

referenceName
    : TOKEN
    ;

referenceChain
    : TOKEN
    ;
