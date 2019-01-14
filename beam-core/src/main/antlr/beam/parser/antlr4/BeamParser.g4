parser grammar BeamParser;

import BeamReferenceParser;

options { tokenVocab = BeamLexer; }

beamFile : file* EOF;
file     : (provider | keyValue | resource | forStmt | importStmt | state);
blockEnd : END;

provider     : PROVIDER providerName providerBody blockEnd;
providerName : IDENTIFIER;
providerBody : keySimpleValue*;

state     : STATE stateName stateBody blockEnd;
stateName : IDENTIFIER;
stateBody : keySimpleValue*;

resource     : resourceType resourceName resourceBody* blockEnd;
resourceBody : keyValue | subresource | forStmt;

subresource     : resourceType resourceName? subresourceBody* blockEnd;
subresourceBody : keyValue;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | stringValue;

importStmt  : IMPORT importPath AS? importName?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

forStmt      : FOR forVariables IN listValue forBody* blockEnd;
forBody      : keyValue | resource | subresource;
forVariables : forVariable (COMMA forVariable)*;
forVariable  : IDENTIFIER;

// -- Key/Value blocks.
//
// Regular key/value blocks can have values that contain references.
// Simple key/value blocks cannot have value references.
keyValue       : key value;
keySimpleValue : key simpleValue;
key            : (IDENTIFIER | STRING_LITERAL | keywords) keyDelimiter;
keywords       : IMPORT | PROVIDER | AS | STATE;
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
