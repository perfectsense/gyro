parser grammar BeamParser;

options { tokenVocab = BeamLexer; }

beamFile : statement  (NEWLINE
           statement)* NEWLINE?
           EOF;

statement
    : keyValueStatement
    | resource
    | forStmt
    | ifStmt
    | importStmt
    | virtualResource
    ;

resource     : resourceType resourceName? NEWLINE resourceBody* END;
resourceBody : (keyValueStatement | resource | forStmt | ifStmt) NEWLINE;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | stringValue;

importStmt  : IMPORT importPath (AS importName)?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

virtualResource      : VR virtualResourceName NEWLINE virtualResourceParam* DEFINE NEWLINE virtualResourceBody* END;
virtualResourceParam : PARAM IDENTIFIER NEWLINE;
virtualResourceName  : IDENTIFIER;
virtualResourceBody  : (keyValueStatement | resource | forStmt | ifStmt) NEWLINE;

// -- Control Structures

controlBody  : controlStmts*;
controlStmts : (keyValueStatement | resource | forStmt | ifStmt) NEWLINE;

forStmt      : FOR forVariables IN (listValue | referenceValue) NEWLINE controlBody END;
forVariables : forVariable (COMMA forVariable)*;
forVariable  : IDENTIFIER;

ifStmt       : IF expression NEWLINE controlBody (ELSEIF expression NEWLINE controlBody)* (ELSE NEWLINE controlBody)? END;

expression
    : value                          # ValueExpression
    | expression operator expression # ComparisonExpression
    | expression AND expression      # AndExpression
    | expression OR expression       # OrExpression
    ;

operator     : EQ | NOTEQ;

// -- Key/Value blocks.
//
// Regular key/value blocks can have values that contain references.
// Simple key/value blocks cannot have value references.
keyValueStatement : key value;
key            : (IDENTIFIER | STRING_LITERAL | keywords) keyDelimiter;
keywords       : IMPORT | AS | VR | PARAM | DEFINE;
keyDelimiter   : COLON;

// -- Value Types
value        : listValue | mapValue | stringValue | booleanValue | numberValue | referenceValue;
numberValue  : DECIMAL_LITERAL | FLOAT_LITERAL;
booleanValue : TRUE | FALSE;
stringValue  : stringExpression | STRING_LITERAL;
stringExpression : QUOTE stringContents* QUOTE;
stringContents   : referenceBody | DOLLAR | LPAREN | RPAREN | TEXT;

mapValue
    :
    LCURLY NEWLINE?
        (keyValueStatement (COMMA NEWLINE?
         keyValueStatement)*      NEWLINE?)?
    RCURLY
    ;

listValue : LBRACKET NEWLINE? (listItemValue (COMMA NEWLINE? listItemValue)* NEWLINE?)? RBRACKET;

listItemValue : stringValue | booleanValue | numberValue | referenceValue;

referenceValue     : DOLLAR LPAREN referenceBody RPAREN ;
referenceBody      : referenceType referenceName (PIPE queryExpression)* | referenceType referenceName | referenceType;
referenceType      : IDENTIFIER (DOT IDENTIFIER)* ;
referenceName      : ( (SLASH | GLOB | IDENTIFIER)* | stringExpression | IDENTIFIER (DOT IDENTIFIER)*) ;

queryExpression
    : queryField                          # QueryFieldValue
    | queryField operator queryValue      # QueryComparisonExpression
    | queryExpression AND queryExpression # QueryAndExpression
    | queryExpression OR queryExpression  # QueryOrExpression
    ;

queryField : IDENTIFIER (DOT IDENTIFIER)*;
queryValue : referenceValue | stringValue | booleanValue | numberValue;
