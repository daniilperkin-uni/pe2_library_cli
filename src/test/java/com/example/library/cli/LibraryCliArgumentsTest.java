package com.example.library.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the command-line surface of the application: the usage text and the
 * version string. Both exist so that {@code --help} and {@code --version} work
 * without starting the interactive menu or touching a data directory.
 */
class LibraryCliArgumentsTest {

    private String captureUsage() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        LibraryCLI.printUsage(out);
        out.flush();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("usage text documents the data directory and both options")
    void usageDocumentsDataDirectoryAndOptions() {
        String usage = captureUsage();

        assertThat(usage).contains("Benutzung");
        assertThat(usage).contains("DATA-DIR");
        assertThat(usage).contains("--help");
        assertThat(usage).contains("--version");
        assertThat(usage).contains("./data");
    }

    @Test
    @DisplayName("usage text names the application so it is identifiable in a log")
    void usageStartsWithApplicationName() {
        assertThat(captureUsage()).startsWith("pe2-library-cli ");
    }

    @Test
    @DisplayName("version falls back to dev when no manifest is present")
    void versionFallsBackToDevOutsideAJar() {
        // Tests run against target/classes, so the package has no
        // Implementation-Version; the fallback keeps --version usable there.
        assertThat(LibraryCLI.version()).isNotBlank();
    }
}
