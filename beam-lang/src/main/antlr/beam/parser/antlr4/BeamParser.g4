parser grammar BeamParser;

options { tokenVocab = BeamLexer; }

beam_root
    : block* EOF
    ;

block
    :  (key_value_block | resource_block | for_block)
    ;

block_end
    : END
    ;

resource_block
    : resource_type resource_name block* block_end
    ;

resource_type
    : IDENTIFIER
    ;

resource_name
    : IDENTIFIER
    | literal_value
    ;

for_block
    : FOR for_list IN list_value block* block_end
    ;

for_list
    : for_list_item (COMMA for_list_item)*
    ;

for_list_item
    : IDENTIFIER
    ;

key_value_block
    : key value
    ;

key
    : (IDENTIFIER | STRING_LITERAL) key_delimiter
    ;

key_delimiter
    : COLON
    ;

value
    : list_value
    | map_value
    | literal_value
    | boolean_value
    | number_value
    | reference_value
    ;

number_value
    : DECIMAL_LITERAL
    | FLOAT_LITERAL
    ;

boolean_value
    : TRUE | FALSE
    ;

literal_value
    : STRING_INTEPRETED
    | STRING_LITERAL
    ;

map_value
    : LCURLY key_value_block? (COMMA key_value_block)* RCURLY
    ;

list_value
    : LBRACKET list_item_value? (COMMA list_item_value)* RBRACKET
    ;

list_item_value
    : literal_value
    | reference_value
    ;

reference_value
    : DOLLAR LPAREN reference_body RPAREN
    ;

reference_body
    : (reference_type reference_name) | (reference_type reference_name PIPE reference_attribute)
    ;

reference_type : IDENTIFIER ;
reference_name : IDENTIFIER ;
reference_attribute : IDENTIFIER ;
