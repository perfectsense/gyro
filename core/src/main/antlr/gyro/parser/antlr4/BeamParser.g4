parser grammar BeamParser;

options { tokenVocab = BeamLexer; }

root
    :
    statement  (NEWLINE
    statement)* NEWLINE?
    EOF
    ;

statement
    : keyValueStatement
    | resource
    | forStatement
    | ifStatement
    | importStmt
    | virtualResource
    ;

blockBody : (blockStatement NEWLINE)*;

blockStatement
    : keyValueStatement
    | resource
    | forStatement
    | ifStatement
    ;

resource : resourceType resourceName? NEWLINE blockBody END;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | stringValue;

importStmt  : IMPORT importPath (AS importName)?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

virtualResource      : VR virtualResourceName NEWLINE virtualResourceParam* DEFINE NEWLINE blockBody END;
virtualResourceParam : PARAM IDENTIFIER NEWLINE;
virtualResourceName  : IDENTIFIER;

// -- Control Structures

forVariable : IDENTIFIER;

forStatement
    :
    FOR forVariable (COMMA forVariable)* IN (listValue | referenceValue) NEWLINE
        blockBody
    END
    ;

ifStatement
    :
    IF condition NEWLINE
        blockBody
    (ELSEIF condition NEWLINE
        blockBody)*
    (ELSE NEWLINE
        blockBody)?
    END
    ;

comparisonOperator : EQ | NOTEQ;

condition
    : value                             # ValueCondition
    | value comparisonOperator value    # ComparisonCondition
    | condition AND condition           # AndCondition
    | condition OR condition            # OrCondition
    ;

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
        keyValueStatement)*       NEWLINE?)?
    RCURLY
    ;

listValue : LBRACKET NEWLINE? (listItemValue (COMMA NEWLINE? listItemValue)* NEWLINE?)? RBRACKET;

listItemValue : stringValue | booleanValue | numberValue | referenceValue;

referenceValue     : DOLLAR LPAREN referenceBody RPAREN ;
referenceBody      : referenceType referenceName (PIPE queryExpression)* | referenceType referenceName | referenceType;
referenceType      : IDENTIFIER (DOT IDENTIFIER)* ;
referenceName      : ( (SLASH | GLOB | IDENTIFIER)* | stringExpression | IDENTIFIER (DOT IDENTIFIER)*) ;

queryExpression
    : queryField                               # QueryFieldValue
    | queryField comparisonOperator queryValue # QueryComparisonExpression
    | queryExpression AND queryExpression      # QueryAndExpression
    | queryExpression OR queryExpression       # QueryOrExpression
    ;

queryField : IDENTIFIER (DOT IDENTIFIER)*;
queryValue : referenceValue | stringValue | booleanValue | numberValue;
