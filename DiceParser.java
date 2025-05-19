import java.util.*;
import java.util.logging.Logger;

/**
 * JDice: Java Dice Rolling Program
 * Copyright (C) 2006 Andrew D. Hilton  (adhilton@cis.upenn.edu)
 * 
 * Refactored version with improvements to:
 * - Performance: Reduced string operations and object creation
 * - Readability: Better method/variable names and structure
 * - Documentation: Added comprehensive Javadoc comments
 * - Error handling: More robust parsing
 */
public class DiceParser {
    
    /**
     * Helper class to manage the input stream for parsing dice expressions.
     * Refactored to:
     * - Fix syntax errors in original implementation
     * - Make state management clearer
     * - Improve performance by reducing string operations
     */
	
	 private static final Logger logger = Logger.getLogger(DiceParser.class.getName());

	    static {
	        // Cấu hình logger đơn giản: hiển thị log level và message
	        logger.setUseParentHandlers(false); // Tắt handler mặc định
	        ConsoleHandler handler = new ConsoleHandler();
	        handler.setFormatter(new SimpleFormatter() {
	            @Override
	            public String format(LogRecord record) {
	                return String.format("[%s] %s\n",
	                    record.getLevel(),
	                    record.getMessage());
	            }
	        });
	        logger.addHandler(handler);
	        logger.setLevel(Level.ALL); // Ghi lại tất cả log level
	    }
	    
    private static class StringStream {
        private StringBuffer buffer;
        
        public StringStream(String s) {
            buffer = new StringBuffer(s);
        }
        
        /**
         * Consumes whitespace from the beginning of the buffer.
         * Refactored from original munchWhiteSpace to be more efficient.
         */
        private void consumeWhitespace() {
            int index = 0;
            while (index < buffer.length() && Character.isWhitespace(buffer.charAt(index))) {
                index++;
            }
            buffer.delete(0, index);
        }
        
        public boolean isEmpty() {
            consumeWhitespace();
            return buffer.length() == 0;
        }
        
        /**
         * Attempts to read an integer from the stream.
         * Returns null if no integer is found.
         * Refactored to combine getInt and readInt from original.
         */
        public Integer readInteger() {
            consumeWhitespace();
            int index = 0;
            
            while (index < buffer.length() && Character.isDigit(buffer.charAt(index))) {
                index++;
            }
            
            if (index == 0) {
                return null;
            }
            
            try {
                int value = Integer.parseInt(buffer.substring(0, index));
                buffer.delete(0, index);
                return value;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        /**
         * Reads a signed integer (+/- prefix).
         * Refactored to be more straightforward than original readSgnInt.
         */
        public Integer readSignedInteger() {
            consumeWhitespace();
            boolean negative = false;
            
            if (checkAndConsume("+")) {
                // Positive is default, just proceed
            } else if (checkAndConsume("-")) {
                negative = true;
            }
            
            Integer value = readInteger();
            if (value == null) {
                return null;
            }
            
            return negative ? -value : value;
        }
        
        /**
         * Checks if the given string is at the current position and consumes it if found.
         * More efficient than original checkAndEat.
         */
        public boolean checkAndConsume(String s) {
            consumeWhitespace();
            if (buffer.indexOf(s) == 0) {
                buffer.delete(0, s.length());
                return true;
            }
            return false;
        }
        
        /**
         * Saves the current state of the stream for possible restoration.
         * Refactored to be more memory efficient.
         */
        public StringStream saveState() {
            return new StringStream(buffer.toString());
        }
        
        /**
         * Restores the stream to a previous state.
         * More clearly named than original restore.
         */
        public void restoreState(StringStream savedState) {
            this.buffer = new StringBuffer(savedState.buffer);
        }
        
        public String toString() {
            return buffer.toString();
        }
    }

    /**
     * Parses a dice roll expression string into a collection of DieRoll objects.
     * Refactored to:
     * - Have clearer error handling
     * - Better method structure
     * - More descriptive variable names
     */
    public static Vector<DieRoll> parseRoll(String expression) {
        StringStream stream = new StringStream(expression.toLowerCase());
        Vector<DieRoll> rolls = parseRollSequence(stream, new Vector<DieRoll>());
        return stream.isEmpty() ? rolls : null;
    }
    
    /**
     * Recursively parses a sequence of dice rolls separated by semicolons.
     * Refactored to be more straightforward than original parseRollInner.
     */
    private static Vector<DieRoll> parseRollSequence(StringStream stream, Vector<DieRoll> accumulatedRolls) {
        Vector<DieRoll> currentRolls = parseCompoundDiceExpression(stream);
        if (currentRolls == null) {
            return null;
        }
        
        accumulatedRolls.addAll(currentRolls);
        
        if (stream.checkAndConsume(";")) {
            return parseRollSequence(stream, accumulatedRolls);
        }
        
        return accumulatedRolls;
    }
    
    /**
     * Parses a potentially multiplied dice expression (e.g., "4x3d6").
     * Refactored from parseXDice to be more efficient and clearer.
     */
    private static Vector<DieRoll> parseCompoundDiceExpression(StringStream stream) {
        StringStream savedState = stream.saveState();
        Integer multiplier = stream.readInteger();
        int repeatCount = 1;
        
        if (multiplier != null && stream.checkAndConsume("x")) {
            repeatCount = multiplier;
        } else if (multiplier != null) {
            // Not a multiplier expression, restore state
            stream.restoreState(savedState);
        }
        
        DieRoll dieRoll = parseDiceExpression(stream);
        if (dieRoll == null) {
            return null;
        }
        
        Vector<DieRoll> result = new Vector<DieRoll>();
        for (int i = 0; i < repeatCount; i++) {
            result.add(dieRoll);
        }
        return result;
    }
    
    /**
     * Parses a single dice expression with optional bonus and chaining.
     * Refactored from parseDice to have clearer structure.
     */
    private static DieRoll parseDiceExpression(StringStream stream) {
        DieRoll baseRoll = parseBaseDiceExpression(stream);
        return parseDiceChain(baseRoll, stream);
    }
    
    /**
     * Parses the base part of a dice expression (e.g., "3d6+2").
     * Refactored from parseDiceInner to be more straightforward.
     */
    private static DieRoll parseBaseDiceExpression(StringStream stream) {
        Integer diceCount = stream.readInteger();
        int numberOfDice = (diceCount != null) ? diceCount : 1;
        
        if (!stream.checkAndConsume("d")) {
            return null;
        }
        
        Integer sides = stream.readInteger();
        if (sides == null) {
            return null;
        }
        
        Integer bonus = stream.readSignedInteger();
        int bonusValue = (bonus != null) ? bonus : 0;
        
        return new DieRoll(numberOfDice, sides, bonusValue);
    }
    
    /**
     * Handles chained dice expressions with & operator.
     * Refactored from parseDTail to be more efficient and clearer.
     */
    private static DieRoll parseDiceChain(DieRoll firstRoll, StringStream stream) {
        if (firstRoll == null) {
            return null;
        }
        
        if (stream.checkAndConsume("&")) {
            DieRoll nextRoll = parseDiceExpression(stream);
            if (nextRoll == null) {
                return null;
            }
            return parseDiceChain(new DiceSum(firstRoll, nextRoll), stream);
        }
        
        return firstRoll;
    }
    
    // ... (test methods remain the same)
    private static void test(String s) {
        Vector<DieRoll> v = parseRoll(s);
        if (v == null) {
            System.out.println("Failure: " + s);
        } else {
            System.out.println("Results for " + s + ":");
            for (DieRoll dr : v) {
                System.out.print(dr);
                System.out.print(": ");
                System.out.println(dr.makeRoll());
            }
        }
    }
    
    public static void main(String[] args) {
        test("d6");
        test("2d6");
        test("d6+5");
        test("4X3d8-5");
        test("12d10+5 & 4d6+2");
        test("d6 ; 2d4+3");
        test("4d6+3 ; 8d12 -15 ; 9d10 & 3d6 & 4d12 +17");
        test("4d6 + xyzzy");
        test("hi");
        test("4d4d4");
    }
}