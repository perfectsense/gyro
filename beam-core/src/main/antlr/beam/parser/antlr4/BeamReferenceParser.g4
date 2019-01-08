parser grammar BeamReferenceParser;

options { tokenVocab = BeamLexer; }

reference_value
    : DOLLAR LPAREN reference_body RPAREN
    ;

reference_body
    : (reference_type reference_name?) | (reference_type reference_name PIPE reference_attribute)
    ;

reference_type : IDENTIFIER ;
reference_name : (string_expression | IDENTIFIER (DOT IDENTIFIER)*) ;
reference_attribute : IDENTIFIER (DOT IDENTIFIER)*;

string_expression
    : QUOTE string_contents* QUOTE
    ;

string_contents
    : reference_body
    | DOLLAR
    | LPAREN | RPAREN
    | TEXT
    ;