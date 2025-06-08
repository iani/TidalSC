// 日  8  6 2025 14:55
// A parser of Tidal syntax, written by Cursor AI
// (https://www.cursor.com)
// Method parsePattern is instructive as an example of
// how to parse mini-notation type syntax.
// It contains inconsistencies compared to TidalCycles
// as documented in https://tidalcycles.org/docs/reference/cycles.
// The translation into sclang patterns (method convertToPattern)
// is useless.  However the parsePattern is quite a useful
// start for constructing a more correct parser, and
// is a good example of how to write parsers.

TidalParser {
    classvar <version = "0.1.0";
    
    // Token types
    classvar <TOKEN_NUMBER = \number;
    classvar <TOKEN_STRING = \string;
    classvar <TOKEN_LIST_START = \listStart;
    classvar <TOKEN_LIST_END = \listEnd;
    classvar <TOKEN_ANGLE_START = \angleStart;
    classvar <TOKEN_ANGLE_END = \angleEnd;
    classvar <TOKEN_OPERATOR = \operator;
    classvar <TOKEN_WHITESPACE = \whitespace;
    classvar <TOKEN_SYMBOL = \symbol;
    classvar <TOKEN_DOT = \dot;
    classvar <TOKEN_COLON = \colon;
    classvar <TOKEN_SLASH = \slash;
    classvar <TOKEN_STAR = \star;
    classvar <TOKEN_PLUS = \plus;
    classvar <TOKEN_MINUS = \minus;
    classvar <TOKEN_MODULO = \modulo;
    classvar <TOKEN_TILDE = \tilde;
    
    // Pattern types
    classvar <PATTERN_SEQUENCE = \sequence;
    classvar <PATTERN_PARALLEL = \parallel;
    classvar <PATTERN_TRANSFORM = \transform;
    classvar <PATTERN_EUCLIDEAN = \euclidean;
    classvar <PATTERN_MODULATION = \modulation;
    classvar <PATTERN_OPERATION = \operation;
    
    // Transformation types
    classvar <TRANSFORM_REV = \rev;
    classvar <TRANSFORM_PALINDROME = \palindrome;
    classvar <TRANSFORM_ITER = \iter;
    classvar <TRANSFORM_STRETCH = \stretch;
    classvar <TRANSFORM_FAST = \fast;
    classvar <TRANSFORM_SLOW = \slow;
    classvar <TRANSFORM_DEGRADE = \degrade;
    classvar <TRANSFORM_DENSITY = \density;
    classvar <TRANSFORM_CHOP = \chop;
    classvar <TRANSFORM_OFFSET = \offset;
    classvar <TRANSFORM_JUMP = \jump;
    classvar <TRANSFORM_EVERY = \every;
    classvar <TRANSFORM_WHEN = \when;
    classvar <TRANSFORM_SOMETIMES = \sometimes;
    
    var <tokens;
    var <currentIndex;
    var <modulationDepth;
    
    *new {
        ^super.newCopyArgs.init;
    }
    
    init {
        tokens = List.new;
        currentIndex = 0;
        modulationDepth = 0;
    }
    
    // Main parsing method
    parse { |patternString|
        this.tokenize(patternString);
        currentIndex = 0;
        ^this.parsePattern;
    }
    
    // Tokenize the input string
    tokenize { |patternString|
        tokens.clear;
        
        var i = 0;
        var currentToken = "";
        var inString = false;
        
        while { i < patternString.size } {
            var char = patternString[i];
            
            case
            { char == $[ } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_LIST_START, "["]);
            }
            { char == $] } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_LIST_END, "]"]);
            }
            { char == $< } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_ANGLE_START, "<"]);
            }
            { char == $> } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_ANGLE_END, ">"]);
            }
            { char == $. } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_DOT, "."]);
            }
            { char == $: } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_COLON, ":"]);
            }
            { char == $/ } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_SLASH, "/"]);
            }
            { char == $* } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_STAR, "*"]);
            }
            { char == $+ } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_PLUS, "+"]);
            }
            { char == $- } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_MINUS, "-"]);
            }
            { char == $% } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_MODULO, "%"]);
            }
            { char == $~ } {
                if(currentToken.size > 0) {
                    this.addToken(currentToken);
                    currentToken = "";
                };
                tokens.add([TOKEN_TILDE, "~"]);
            }
            { char == $" } {
                inString = inString.not;
                currentToken = currentToken ++ char;
            }
            { char == $  } {
                if(inString) {
                    currentToken = currentToken ++ char;
                } {
                    if(currentToken.size > 0) {
                        this.addToken(currentToken);
                        currentToken = "";
                    };
                };
            }
            { true } {
                currentToken = currentToken ++ char;
            };
            
            i = i + 1;
        };
        
        if(currentToken.size > 0) {
            this.addToken(currentToken);
        };
    }
    
    // Add a token to the token list
    addToken { |token|
        var type = this.classifyToken(token);
        tokens.add([type, token]);
    }
    
    // Classify a token into its type
    classifyToken { |token|
        if(token[0] == $") {
            ^TOKEN_STRING;
        };
        
        if(token.tryPerform(\asFloat).notNil) {
            ^TOKEN_NUMBER;
        };
        
        if(token.size == 1) {
            case
            { token == "." } { ^TOKEN_DOT }
            { token == ":" } { ^TOKEN_COLON }
            { token == "/" } { ^TOKEN_SLASH }
            { token == "*" } { ^TOKEN_STAR }
            { token == "+" } { ^TOKEN_PLUS }
            { token == "-" } { ^TOKEN_MINUS }
            { token == "%" } { ^TOKEN_MODULO }
            { token == "~" } { ^TOKEN_TILDE }
            { true } { ^TOKEN_SYMBOL };
        };
        
        ^TOKEN_SYMBOL;
    }
    
    // Parse a pattern
    parsePattern {
        var result = List.new;
        
        while { currentIndex < tokens.size } {
            var token = tokens[currentIndex];
            
            case
            { token[0] == TOKEN_LIST_START } {
                currentIndex = currentIndex + 1;
                result.add([PATTERN_SEQUENCE, this.parseList]);
            }
            { token[0] == TOKEN_ANGLE_START } {
                currentIndex = currentIndex + 1;
                result.add([PATTERN_PARALLEL, this.parseAngleBrackets]);
            }
            { token[0] == TOKEN_DOT } {
                currentIndex = currentIndex + 1;
                result.add([PATTERN_TRANSFORM, this.parseTransform]);
            }
            { token[0] == TOKEN_SLASH } {
                currentIndex = currentIndex + 1;
                result.add([PATTERN_EUCLIDEAN, this.parseEuclidean]);
            }
            { token[0] == TOKEN_TILDE } {
                currentIndex = currentIndex + 1;
                result.add([PATTERN_MODULATION, this.parseModulation]);
            }
            { token[0] == TOKEN_STAR or: { token[0] == TOKEN_PLUS } or: { token[0] == TOKEN_MINUS } or: { token[0] == TOKEN_MODULO } } {
                result.add([PATTERN_OPERATION, this.parseOperation]);
            }
            { token[0] == TOKEN_LIST_END } {
                ^result;
            }
            { token[0] == TOKEN_ANGLE_END } {
                ^result;
            }
            { token[0] == TOKEN_WHITESPACE } {
                currentIndex = currentIndex + 1;
            }
            { true } {
                result.add(this.parseValue(token));
                currentIndex = currentIndex + 1;
            };
        };
        
        ^result;
    }
    
    // Parse a list (square brackets)
    parseList {
        var result = this.parsePattern;
        if(currentIndex < tokens.size and: { tokens[currentIndex][0] == TOKEN_LIST_END }) {
            currentIndex = currentIndex + 1;
        };
        ^result;
    }
    
    // Parse angle brackets (parallel patterns)
    parseAngleBrackets {
        var result = this.parsePattern;
        if(currentIndex < tokens.size and: { tokens[currentIndex][0] == TOKEN_ANGLE_END }) {
            currentIndex = currentIndex + 1;
        };
        ^result;
    }
    
    // Parse transformations (after dot)
    parseTransform {
        var transform = List.new;
        while { currentIndex < tokens.size } {
            var token = tokens[currentIndex];
            if(token[0] == TOKEN_WHITESPACE) {
                currentIndex = currentIndex + 1;
            } {
                transform.add(this.parseValue(token));
                currentIndex = currentIndex + 1;
            };
        };
        ^transform;
    }
    
    // Parse euclidean patterns (after slash)
    parseEuclidean {
        var result = List.new;
        while { currentIndex < tokens.size } {
            var token = tokens[currentIndex];
            if(token[0] == TOKEN_WHITESPACE) {
                currentIndex = currentIndex + 1;
            } {
                result.add(this.parseValue(token));
                currentIndex = currentIndex + 1;
            };
        };
        ^result;
    }
    
    // Parse modulation (after tilde)
    parseModulation {
        var result = List.new;
        modulationDepth = modulationDepth + 1;
        
        while { currentIndex < tokens.size } {
            var token = tokens[currentIndex];
            if(token[0] == TOKEN_WHITESPACE) {
                currentIndex = currentIndex + 1;
            } {
                result.add(this.parseValue(token));
                currentIndex = currentIndex + 1;
            };
        };
        
        modulationDepth = modulationDepth - 1;
        ^result;
    }
    
    // Parse operations (after operators)
    parseOperation {
        var operation = tokens[currentIndex][0];
        currentIndex = currentIndex + 1;
        var operand = this.parseValue(tokens[currentIndex]);
        currentIndex = currentIndex + 1;
        ^[operation, operand];
    }
    
    // Parse a value
    parseValue { |token|
        case
        { token[0] == TOKEN_NUMBER } {
            ^token[1].asFloat;
        }
        { token[0] == TOKEN_STRING } {
            ^token[1].strip($");
        }
        { token[0] == TOKEN_OPERATOR } {
            ^token[1];
        }
        { token[0] == TOKEN_SYMBOL } {
            ^token[1].asSymbol;
        }
        { true } {
            ^token[1];
        };
    }
    
    // Convert to SuperCollider pattern
    toPattern { |patternString|
        var parsed = this.parse(patternString);
        ^this.convertToPattern(parsed);
    }
    
    // Convert parsed data to SuperCollider pattern
    convertToPattern { |data|
        if(data.isKindOf(List)) {
            if(data.size > 0 and: { data[0].isKindOf(List) and: { data[0][0].isKindOf(Symbol) } }) {
                var type = data[0][0];
                var content = data[0][1];
                
                case
                { type == PATTERN_SEQUENCE } {
                    ^Pseq(this.convertToPattern(content), 1);
                }
                { type == PATTERN_PARALLEL } {
                    ^Ppar(this.convertToPattern(content));
                }
                { type == PATTERN_TRANSFORM } {
                    ^this.applyTransform(content);
                }
                { type == PATTERN_EUCLIDEAN } {
                    ^this.applyEuclidean(content);
                }
                { type == PATTERN_MODULATION } {
                    ^this.applyModulation(content);
                }
                { type == PATTERN_OPERATION } {
                    ^this.applyOperation(content);
                }
                { true } {
                    ^Pseq(this.convertToPattern(data), 1);
                };
            } {
                var elements = data.collect { |elem| this.convertToPattern(elem) };
                ^Pseq(elements, 1);
            };
        } {
            ^data;
        };
    }
    
    // Apply pattern transformations
    applyTransform { |transform|
        var pattern = transform[0];
        var operation = transform[1];
        
        case
        { operation == \rev } {
            ^Pseq(pattern.reverse, 1);
        }
        { operation == \palindrome } {
            ^Pseq(pattern ++ pattern.reverse, 1);
        }
        { operation == \iter } {
            ^Pseq(pattern, inf);
        }
        { operation == \stretch } {
            var factor = transform[2] ? 2;
            ^Pstretch(factor, pattern);
        }
        { operation == \fast } {
            var factor = transform[2] ? 2;
            ^Pfast(factor, pattern);
        }
        { operation == \slow } {
            var factor = transform[2] ? 2;
            ^Pslow(factor, pattern);
        }
        { operation == \degrade } {
            var amount = transform[2] ? 0.5;
            ^Pdegrade(pattern, amount);
        }
        { operation == \density } {
            var amount = transform[2] ? 0.5;
            ^Pdensity(amount, pattern);
        }
        { operation == \chop } {
            var n = transform[2] ? 4;
            ^Pchop(n, pattern);
        }
        { operation == \offset } {
            var amount = transform[2] ? 0.25;
            ^Poffset(amount, pattern);
        }
        { operation == \jump } {
            var amount = transform[2] ? 0.25;
            ^Pjump(amount, pattern);
        }
        { operation == \every } {
            var n = transform[2] ? 4;
            var f = transform[3] ? { |x| x };
            ^Pevery(n, f, pattern);
        }
        { operation == \when } {
            var test = transform[2] ? { true };
            var f = transform[3] ? { |x| x };
            ^Pwhen(test, f, pattern);
        }
        { operation == \sometimes } {
            var amount = transform[2] ? 0.5;
            var f = transform[3] ? { |x| x };
            ^Psometimes(amount, f, pattern);
        }
        { true } {
            ^pattern;
        };
    }
    
    // Apply euclidean rhythm
    applyEuclidean { |euclidean|
        var steps = euclidean[0];
        var pulses = euclidean[1];
        var rotation = euclidean[2] ? 0;
        
        var pattern = this.euclideanRhythm(steps, pulses);
        if(rotation != 0) {
            pattern = pattern.rotate(rotation);
        };
        ^Pseq(pattern, 1);
    }
    
    // Apply modulation
    applyModulation { |modulation|
        var pattern = modulation[0];
        var amount = modulation[1] ? 0.5;
        var rate = modulation[2] ? 1;
        
        ^Pmodulation(pattern, amount, rate);
    }
    
    // Apply operation
    applyOperation { |operation|
        var op = operation[0];
        var operand = operation[1];
        
        case
        { op == TOKEN_STAR } {
            ^Pmul(operand);
        }
        { op == TOKEN_PLUS } {
            ^Padd(operand);
        }
        { op == TOKEN_MINUS } {
            ^Psub(operand);
        }
        { op == TOKEN_MODULO } {
            ^Pmod(operand);
        }
        { true } {
            ^operand;
        };
    }
    
    // Generate euclidean rhythm
    euclideanRhythm { |steps, pulses|
        var pattern = Array.fill(steps, 0);
        var bucket = 0;
        
        pulses.do { |i|
            bucket = bucket + steps;
            if(bucket >= pulses) {
                pattern[i] = 1;
                bucket = bucket - pulses;
            };
        };
        
        ^pattern;
    }
    
    // Class method for quick conversion
    *tidalToSC { |patternString|
        ^this.new.toPattern(patternString);
    }
} 