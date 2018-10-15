parser grammar Beam;
options
   { tokenVocab = BeamLexer; }
/*
 * Parser Rules
 */

beamRoot
    : globalScope EOF
    ;

globalScope
    : ( pluginBlock | importBlock | resourceBlock | assignmentBlock)+
    ;

pluginBlock
    : PLUGIN path
    ;

importBlock
    : IMPORT path AS variable
    ;

resourceBlock
    : resourceProvider variable map
    ;

assignmentBlock
    : LET variable ASSIGN value
    ;

resourceProvider
    : RESOURCE_PROVIDER
    ;

map
    : LBLOCK (keyValueBlock | resourceBlock)* RBLOCK
    ;

list
    : (LBRACET value? (COMMA value)* RBRACET)
    ;

key
    : ID
    ;

value
    : QUOTED_STRING | ID | NUMBERS | list | map | reference
    ;

keyValueBlock
    : key COLON value
    ;

reference
    : REFERENCE LBLOCK referenceChain RBLOCK
    ;

referenceChain
    : ID (DOT ID)*
    ;

variable
    : ID
    ;

path
    : QUOTED_STRING
    ;
