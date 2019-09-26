/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

lexer grammar GyroLexer;

// directive
AT    : '@';
COLON : ':';
COMMA : ',';
MINUS : '-';
END   : 'end';

// value
ASTERISK : '*';
SLASH    : '/';
PERCENT  : '%';
PLUS     : '+';
EQ       : '=';
NE       : '!=';
LT       : '<';
LE       : '<=';
GT       : '>';
GE       : '>=';
AND      : 'and';
OR       : 'or';
DOT      : '.';
LPAREN   : '(' -> pushMode(DEFAULT_MODE);
RPAREN   : ')' -> popMode;

// bool
TRUE  : 'true';
FALSE : 'false';

// list
LBRACKET : '[';
RBRACKET : ']';

// map
LBRACE : '{';
RBRACE : '}';

// number
NUMBERS : [0-9] [_0-9]*;

// reference
DOLLAR : '$';
BAR    : '|';

// string
STRING : '\'' ~('\'')* '\'';
DQUOTE : '"' -> pushMode(STRING_MODE);

KEYWORD     : AND | OR;
WHITESPACES : [ \t]+ -> channel(HIDDEN);
NEWLINE     : [\n\r];
COMMENT     : '#' ~[\n\r]* -> channel(HIDDEN);
IDENTIFIER  : [A-Z_a-z] [-/0-9A-Z_a-z]*;

mode STRING_MODE;

S_DOLLAR     : [$\\] -> type(DOLLAR);
S_LPAREN     : '(' -> type(LPAREN), pushMode(DEFAULT_MODE);
S_IDENTIFIER : [A-Z_a-z] [-/0-9A-Z_a-z]* -> type(IDENTIFIER);
S_DQUOTE     : '"' -> type(DQUOTE), popMode;
S_NEWLINE    : [\n\r] -> type(NEWLINE);
CHARACTER    : .;
