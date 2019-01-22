parser grammar BeamParser;

import BeamReferenceParser;

options { tokenVocab = BeamLexer; }

beamFile : file* EOF;
file     : (plugin | keyValue | resource | forStmt | ifStmt | importStmt | state | virtualResource);
blockEnd : END;

plugin     : PLUGIN pluginBody blockEnd;
pluginBody : keySimpleValue*;

state     : STATE stateName stateBody blockEnd;
stateName : IDENTIFIER;
stateBody : keySimpleValue*;

resource     : resourceType resourceName resourceBody* blockEnd;
resourceBody : keyValue | subresource | forStmt | ifStmt;

subresource     : resourceType resourceName? subresourceBody* blockEnd;
subresourceBody : keyValue;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | stringValue;

importStmt  : IMPORT importPath AS? importName?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

virtualResource      : VR virtualResourceName virtualResourceParam* DEFINE virtualResourceBody* blockEnd;
virtualResourceParam : PARAM IDENTIFIER;
virtualResourceName  : IDENTIFIER;
virtualResourceBody  : keyValue | resource | forStmt | ifStmt;

// -- Control Structures

controlBody  : controlStmts*;
controlStmts : keyValue | resource | subresource | forStmt | ifStmt;

forStmt      : FOR forVariables IN (listValue | referenceValue) controlBody blockEnd;
forVariables : forVariable (COMMA forVariable)*;
forVariable  : IDENTIFIER;

ifStmt       : IF expression controlBody (ELSEIF expression controlBody)* (ELSE controlBody)? blockEnd;

expression
    : value                          # ValueExpression
    | expression operator expression # ComparisonExpression
    | expression OR expression       # OrExpression
    | expression AND expression      # AndExpression
    ;

operator     : EQ | NOTEQ;

// -- Key/Value blocks.
//
// Regular key/value blocks can have values that contain references.
// Simple key/value blocks cannot have value references.
keyValue       : key value;
keySimpleValue : key simpleValue;
key            : (IDENTIFIER | STRING_LITERAL | keywords) keyDelimiter;
keywords       : IMPORT | PLUGIN | AS | STATE | VR | PARAM | DEFINE;
keyDelimiter   : COLON;

// -- Value Types
value        : listValue | mapValue | stringValue | booleanValue | numberValue | referenceValue;
simpleValue  : listValue | mapValue | STRING_LITERAL | booleanValue | numberValue;
numberValue  : DECIMAL_LITERAL | FLOAT_LITERAL;
booleanValue : TRUE | FALSE;
stringValue  : stringExpression | STRING_LITERAL;

mapValue
    : LCURLY keyValue? (COMMA keyValue)* RCURLY
    | LCURLY keyValue? (COMMA keyValue)* {
          notifyErrorListeners("Extra ',' in map");
      } COMMA RCURLY
    ;

listValue
    : LBRACKET listItemValue? (COMMA listItemValue)* RBRACKET
    | LBRACKET listItemValue? (COMMA listItemValue)* {
        notifyErrorListeners("Extra ',' in list");
      } COMMA RBRACKET
    ;

listItemValue : stringValue | booleanValue | numberValue | referenceValue;
