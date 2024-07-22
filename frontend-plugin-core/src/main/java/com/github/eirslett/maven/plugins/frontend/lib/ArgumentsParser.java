package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class ArgumentsParser {

    private final List<String> additionalArguments;

    ArgumentsParser() {
        this(Collections.<String>emptyList());
    }

    ArgumentsParser(List<String> additionalArguments) {
        this.additionalArguments = additionalArguments;
    }

    /**
     * Parses a given string of arguments, splitting it by characters that are whitespaces according to {@link Character#isWhitespace(char)}.
     * <p>
     * This method respects quoted arguments. Meaning that whitespaces appearing phrases that are enclosed by an opening
     * single or double quote and a closing single or double quote or the end of the string will not be considered.
     * <p>
     * All characters excluding whitespaces considered for splitting stay in place.
     * <p>
     * Examples:
     * "foo bar" will be split to ["foo", "bar"]
     * "foo \"bar foobar\"" will be split to ["foo", "\"bar foobar\""]
     * "foo 'bar" will be split to ["foo", "'bar"]
     *
     * @param args a string of arguments
     * @return an mutable copy of the list of all arguments
     */
    List<String> parse(String args) {
        if (args == null || "null".equals(args) || args.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> arguments = new LinkedList<>();
        final StringBuilder argumentBuilder = new StringBuilder();
        Character quote = null;

        for (int i = 0, l = args.length(); i < l; i++) {
            char c = args.charAt(i);

            if (Character.isWhitespace(c) && quote == null) {
                addArgument(argumentBuilder, arguments);
                continue;
            } else if (c == '"' || c == '\'') {
                // explicit boxing allows us to use object caching of the Character class
                Character currentQuote = Character.valueOf(c);
                if (quote == null) {
                    quote = currentQuote;
                } else if (quote.equals(currentQuote)){
                    quote = null;
                } // else
                // we ignore the case when a quoted argument contains the other kind of quote
            }

            argumentBuilder.append(c);
        }

        addArgument(argumentBuilder, arguments);

        for (String argument : this.additionalArguments) {
            if (!arguments.contains(argument)) {
                arguments.add(argument);
            }
        }

        return new ArrayList<>(arguments);
    }

    private static void addArgument(StringBuilder argumentBuilder, List<String> arguments) {
        if (argumentBuilder.length() > 0) {
            String argument = argumentBuilder.toString();
            arguments.add(argument);
            argumentBuilder.setLength(0);
        }
    }
}
