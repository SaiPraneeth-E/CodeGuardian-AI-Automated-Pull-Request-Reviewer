package com.codeguardian.usecases.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DiffPreprocessorTest {

    @Test
    void preprocessDiff_NullOrEmpty_ReturnsSame() {
        Assertions.assertNull(DiffPreprocessor.preprocessDiff(null));
        Assertions.assertEquals("", DiffPreprocessor.preprocessDiff(""));
        Assertions.assertEquals("   ", DiffPreprocessor.preprocessDiff("   "));
    }

    @Test
    void preprocessDiff_NoDeletedFiles_StripsDeletedLines() {
        String rawDiff = "diff --git a/App.java b/App.java\n" +
                "index 123456..789012 100644\n" +
                "--- a/App.java\n" +
                "+++ b/App.java\n" +
                "@@ -1,5 +1,5 @@\n" +
                " public class App {\n" +
                "-    private int count = 0;\n" +
                "+    private int count = 1;\n" +
                " }";
        String expected = "diff --git a/App.java b/App.java\n" +
                "index 123456..789012 100644\n" +
                "--- a/App.java\n" +
                "+++ b/App.java\n" +
                "@@ -1,5 +1,5 @@\n" +
                " public class App {\n" +
                "+    private int count = 1;\n" +
                " }";
        String processed = DiffPreprocessor.preprocessDiff(rawDiff);
        Assertions.assertEquals(expected.trim(), processed.trim());
    }

    @Test
    void preprocessDiff_WithDeletedFiles_StripsHunks() {
        String rawDiff = "diff --git a/App.java b/App.java\n" +
                "index 123456..789012 100644\n" +
                "--- a/App.java\n" +
                "+++ b/App.java\n" +
                "@@ -1,5 +1,5 @@\n" +
                " public class App {\n" +
                "-    private int count = 0;\n" +
                "+    private int count = 1;\n" +
                " }\n" +
                "diff --git/Deleted.java b/Deleted.java\n" +
                "deleted file mode 100644\n" +
                "index abcdef..000000\n" +
                "--- a/Deleted.java\n" +
                "+++ /dev/null\n" +
                "@@ -1,3 +0,0 @@\n" +
                "-public class Deleted {\n" +
                "-    // content\n" +
                "-}";

        String expected = "diff --git a/App.java b/App.java\n" +
                "index 123456..789012 100644\n" +
                "--- a/App.java\n" +
                "+++ b/App.java\n" +
                "@@ -1,5 +1,5 @@\n" +
                " public class App {\n" +
                "+    private int count = 1;\n" +
                " }\n" +
                "diff --git/Deleted.java b/Deleted.java\n" +
                "deleted file mode 100644\n" +
                "index abcdef..000000\n" +
                "--- a/Deleted.java\n" +
                "+++ /dev/null\n" +
                "(File content stripped from review context)";

        String processed = DiffPreprocessor.preprocessDiff(rawDiff);
        Assertions.assertEquals(expected.trim(), processed.trim());
    }

    @Test
    void preprocessDiff_WithLockFiles_StripsHunks() {
        String rawDiff = "diff --git a/App.java b/App.java\n" +
                "index 123456..789012 100644\n" +
                "--- a/App.java\n" +
                "+++ b/App.java\n" +
                "@@ -1,5 +1,5 @@\n" +
                " public class App {\n" +
                " }\n" +
                "diff --git a/pnpm-lock.yaml b/pnpm-lock.yaml\n" +
                "index 111111..222222 100644\n" +
                "--- a/pnpm-lock.yaml\n" +
                "+++ b/pnpm-lock.yaml\n" +
                "@@ -10,3 +10,6 @@\n" +
                "- old-package: 1.0.0\n" +
                "+ new-package: 1.1.0";

        String expected = "diff --git a/App.java b/App.java\n" +
                "index 123456..789012 100644\n" +
                "--- a/App.java\n" +
                "+++ b/App.java\n" +
                "@@ -1,5 +1,5 @@\n" +
                " public class App {\n" +
                " }\n" +
                "diff --git a/pnpm-lock.yaml b/pnpm-lock.yaml\n" +
                "index 111111..222222 100644\n" +
                "--- a/pnpm-lock.yaml\n" +
                "+++ b/pnpm-lock.yaml\n" +
                "(File content stripped from review context)";

        String processed = DiffPreprocessor.preprocessDiff(rawDiff);
        Assertions.assertEquals(expected.trim(), processed.trim());
    }
}
