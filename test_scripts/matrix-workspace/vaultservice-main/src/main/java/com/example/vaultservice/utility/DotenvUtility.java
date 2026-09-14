package com.example.vaultservice.utility;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DotenvUtility {

    public Map<String, String> parse(InputStream inputStream) throws IOException {
        Map<String, String> variables = new LinkedHashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        int lineNumber = 0;

        while((line = reader.readLine()) != null) {
            lineNumber++;

            if(line.isBlank() || line.stripLeading().startsWith("#"))
                continue;

            if(line.startsWith("export ") || line.startsWith("export\t"))
                throw new IllegalArgumentException("Unsupported dotenv export on line " + lineNumber);

            int equalsIndex = line.indexOf("=");

            if(equalsIndex == -1)
                throw new IllegalArgumentException("Invalid dotenv line " + lineNumber);

            String key = line.substring(0, equalsIndex).trim();
            String value = parseValue(line.substring(equalsIndex + 1), lineNumber);

            validateKey(key);

            if(variables.containsKey(key))
                throw new IllegalArgumentException("Duplicate dotenv key on line " + lineNumber + ": " + key);

            variables.put(key, value);
        }

        return variables;
    }

    public String format(String key, String value) {
        validateKeyAndValue(key, value);

        return key + "=\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"\n";
    }

    public void validateKeyAndValue(String key, String value) {
        validateKey(key);

        if(value == null)
            throw new IllegalArgumentException("Variable value is required");
    }

    private String parseValue(String value, int lineNumber) {
        if(value.startsWith("\""))
            return parseDoubleQuotedValue(value, lineNumber);

        if(value.startsWith("'"))
            return parseSingleQuotedValue(value, lineNumber);

        return value;
    }

    private String parseDoubleQuotedValue(String value, int lineNumber) {
        if(value.length() < 2 || !value.endsWith("\""))
            throw new IllegalArgumentException("Unclosed double quote on line " + lineNumber);

        String quotedValue = value.substring(1, value.length() - 1);
        StringBuilder parsedValue = new StringBuilder();
        boolean escaped = false;

        for(int i = 0; i < quotedValue.length(); i++) {
            char character = quotedValue.charAt(i);

            if(escaped) {
                if(character == '\\')
                    parsedValue.append('\\');
                else if(character == '"')
                    parsedValue.append('"');
                else if(character == 'n')
                    parsedValue.append('\n');
                else if(character == 'r')
                    parsedValue.append('\r');
                else
                    throw new IllegalArgumentException("Unsupported escape on line " + lineNumber);

                escaped = false;
                continue;
            }

            if(character == '\\') {
                escaped = true;
                continue;
            }

            if(character == '"')
                throw new IllegalArgumentException("Unescaped double quote on line " + lineNumber);

            parsedValue.append(character);
        }

        if(escaped)
            throw new IllegalArgumentException("Unsupported escape on line " + lineNumber);

        return parsedValue.toString();
    }

    private String parseSingleQuotedValue(String value, int lineNumber) {
        if(value.length() < 2 || !value.endsWith("'"))
            throw new IllegalArgumentException("Unclosed single quote on line " + lineNumber);

        String quotedValue = value.substring(1, value.length() - 1);

        if(quotedValue.contains("'"))
            throw new IllegalArgumentException("Unescaped single quote on line " + lineNumber);

        return quotedValue;
    }

    private void validateKey(String key) {
        if(key == null || !key.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Variable key is invalid");
    }
}
