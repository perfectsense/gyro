parser grammar BeamParser;

import BeamReferenceParser;

options { tokenVocab = BeamLexer; }

beam_root  : file_block* EOF;
file_block : (provider_block | key_value_block | resource_block | for_block | import_block);
block_end  : END;

provider_block      : PROVIDER provider_name provider_block_body block_end;
provider_name       : IDENTIFIER;
provider_block_body : key_simple_value_block*;

resource_block      : resource_type resource_name resource_block_body* block_end;
resource_block_body : key_value_block | subresource_block | for_block;

subresource_block      : resource_type resource_name? subresource_block_body* block_end;
subresource_block_body : key_value_block;

resource_type : IDENTIFIER;
resource_name : IDENTIFIER | string_value;

import_block : IMPORT import_path AS? import_name?;
import_path  : IDENTIFIER;
import_name  : IDENTIFIER;

for_block      : FOR for_list IN list_value for_block_body* block_end;
for_block_body : key_value_block | resource_block | subresource_block;
for_list       : for_list_item (COMMA for_list_item)*;
for_list_item  : IDENTIFIER;

// -- Key/Value blocks.
//
// Regular key/value blocks can have values that contain references.
// Simple key/value blocks cannot have value references.
key_value_block        : key value;
key_simple_value_block : key simple_value;
key                    : (IDENTIFIER | STRING_LITERAL | keywords) key_delimiter;
keywords               : IMPORT | PROVIDER | AS;
key_delimiter          : COLON;

// -- Value Types
value         : list_value | map_value | string_value | boolean_value | number_value | reference_value;
simple_value  : list_value | map_value | STRING_LITERAL | boolean_value | number_value;
number_value  : DECIMAL_LITERAL | FLOAT_LITERAL;
boolean_value : TRUE | FALSE;
string_value  : string_expression | STRING_LITERAL;

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

list_item_value : string_value | boolean_value | number_value | reference_value;
