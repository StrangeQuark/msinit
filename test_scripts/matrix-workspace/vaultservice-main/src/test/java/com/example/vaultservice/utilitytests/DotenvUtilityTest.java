package com.example.vaultservice.utilitytests;

import com.example.vaultservice.utility.DotenvUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DotenvUtilityTest {
    private final DotenvUtility dotenvUtility = new DotenvUtility();

    @Test
    void parseEnvFileTest() throws Exception {
        String fileContent = "# Comment\n"
                + "SIMPLE=value\n"
                + "SPACES=  value with spaces  \n"
                + "QUOTED=\"A \\\"quoted\\\" value\\nnext\"\n"
                + "LITERAL='line\\nvalue'\n";

        Map<String, String> variables = dotenvUtility.parse(
                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
        );

        Assertions.assertEquals("value", variables.get("SIMPLE"));
        Assertions.assertEquals("  value with spaces  ", variables.get("SPACES"));
        Assertions.assertEquals("A \"quoted\" value\nnext", variables.get("QUOTED"));
        Assertions.assertEquals("line\\nvalue", variables.get("LITERAL"));
    }

    @Test
    void formatEnvFileTest() throws Exception {
        String fileContent = "VALUE=\"A \\\"quoted\\\" value\\nnext\"\n"
                + "PATH='C:\\test\\file'\n";

        Map<String, String> variables = dotenvUtility.parse(
                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
        );

        String formattedFile = "";

        for(Map.Entry<String, String> variable : variables.entrySet())
            formattedFile += dotenvUtility.format(variable.getKey(), variable.getValue());

        Map<String, String> parsedVariables = dotenvUtility.parse(
                new ByteArrayInputStream(formattedFile.getBytes(StandardCharsets.UTF_8))
        );

        Assertions.assertEquals(variables, parsedVariables);
    }

    @Test
    void parseEnvFileRejectsInvalidLinesTest() {
        String fileContent = "export VALUE=value\n";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dotenvUtility.parse(
                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
        ));
    }

    @Test
    void parseEnvFileRejectsUnclosedQuotesTest() {
        String fileContent = "VALUE=\"value\n";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dotenvUtility.parse(
                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
        ));
    }

    @Test
    void parseEnvFileRejectsUnsupportedEscapesTest() {
        String fileContent = "VALUE=\"value\\t\"\n";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dotenvUtility.parse(
                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
        ));
    }

    @Test
    void parseEnvFileRejectsDuplicateKeysTest() {
        String fileContent = "VALUE=first\nVALUE=second\n";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dotenvUtility.parse(
                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
        ));
    }
}
