parser grammar BeamReferenceParser;

options { tokenVocab = BeamLexer; }

referenceValue
    : DOLLAR LPAREN referenceBody RPAREN
    ;

referenceBody
    : (referenceType referenceName?) | (referenceType referenceName PIPE referenceAttribute)
    ;

referenceType : IDENTIFIER (DOT IDENTIFIER)* ;
referenceName : (stringExpression | IDENTIFIER (DOT IDENTIFIER)*) ;
referenceAttribute : IDENTIFIER (DOT IDENTIFIER)*;

stringExpression
    : QUOTE stringContents* QUOTE
    ;

stringContents
    : referenceBody
    | DOLLAR
    | LPAREN | RPAREN
    | TEXT
    ;