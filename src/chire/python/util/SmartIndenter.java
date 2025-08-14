package chire.python.util;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.stream.Collectors;

import java.util.ArrayDeque;
import java.util.Deque;

public class SmartIndenter {
    private final StringBuilder buffer = new StringBuilder();
    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private int currentIndent = 0;
    private boolean atLineStart = true;
    private final String indentString;

    public SmartIndenter(String indentString) {
        this.indentString = indentString;
        indentStack.push(0);
    }

    public SmartIndenter indent() {
        currentIndent++;
        return this;
    }

    public SmartIndenter unindent() {
        if (currentIndent > 0) currentIndent--;
        return this;
    }

    public SmartIndenter pushIndent() {
        indentStack.push(currentIndent);
        return this;
    }

    public SmartIndenter popIndent() {
        if (!indentStack.isEmpty()) {
            currentIndent = indentStack.pop();
        }
        return this;
    }

    public SmartIndenter add(String text) {
        if (text.isEmpty()) return this;

        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (atLineStart && !line.isEmpty()) {
                addIndentation();
            }

            buffer.append(line);

            if (i < lines.length - 1) {
                newLine();
            }
        }

        return this;
    }

    public SmartIndenter addLine(String text){
        return add(text).newLine();
    }

    public SmartIndenter newLine() {
        buffer.append("\n");
        atLineStart = true;
        return this;
    }

    private void addIndentation() {
        for (int i = 0; i < currentIndent; i++) {
            buffer.append(indentString);
        }
        atLineStart = false;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}