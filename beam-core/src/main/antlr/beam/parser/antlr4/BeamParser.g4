parser grammar BeamParser;

options { tokenVocab = BeamLexer; }

beam_root
    : root_block* EOF
    ;

root_block
    :  (key_value_block | resource_block | for_block)
    ;

block_end
    : END
    ;

resource_block
    : resource_type resource_name resource_block_body* block_end
    ;

resource_block_body
    : key_value_block
    | subresource_block
    ;

subresource_block
    : resource_type resource_name? subresource_block_body* block_end
    ;

subresource_block_body
    : key_value_block
    ;

resource_type
    : IDENTIFIER
    ;

resource_name
    : IDENTIFIER
    | literal_value
    ;

for_block
    : FOR for_list IN list_value for_block_body* block_end
    ;

for_block_body
    : key_value_block
    | resource_block
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
    | LCURLY key_value_block? (COMMA key_value_block)* {
          notifyErrorListeners("Extra ',' in map");
      } COMMA RCURLY
    ;

list_value
    : LBRACKET list_item_value? (COMMA list_item_value)* RBRACKET
    | LBRACKET list_item_value? (COMMA list_item_value)* {
        notifyErrorListeners("Extra ',' in list");
      } COMMA RBRACKET
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
