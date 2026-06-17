package com.codeguardian.usecases.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to optimize git diff content sizes before passing them to RAG or LLM APIs.
 */
public class DiffPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(DiffPreprocessor.class);

    /**
     * Preprocesses a git diff string to reduce its size:
     * - Identifies completely deleted files (e.g., contains "deleted file mode" or "+++ /dev/null").
     * - Identifies package lock/dependency files (e.g., pnpm-lock.yaml, package-lock.json).
     * - Keeps only the header details for these files and discards the actual diff hunk content.
     * - For modified files, strips out deleted lines (lines starting with '-' but not '---') to save tokens.
     */
    public static String preprocessDiff(String rawDiff) {
        if (rawDiff == null || rawDiff.trim().isEmpty()) {
            return rawDiff;
        }

        // Split raw diff using lookahead for "diff --git" followed by space or slash to isolate individual files
        String[] parts = rawDiff.split("(?=diff --git[ /])");
        StringBuilder processed = new StringBuilder();
        int optimizedFilesCount = 0;
        int originalLength = rawDiff.length();

        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }

            if (isDeletedFile(part) || isLockFile(part)) {
                processed.append(extractHeader(part)).append("\n");
                optimizedFilesCount++;
            } else {
                // Strip deleted lines from modified files to further reduce token count
                processed.append(cleanModifiedFileDiff(part));
            }
        }

        String result = processed.toString().trim();

        // 1. Remove context lines if preprocessed diff size is still > 6,000 characters
        if (result.length() > 6000) {
            String[] lines = result.split("\\r?\\n");
            StringBuilder noContext = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith(" ")) {
                    continue;
                }
                noContext.append(line).append("\n");
            }
            result = noContext.toString().trim();
        }

        // 2. Truncate to hard safety limit if it still exceeds 6,000 characters
        if (result.length() > 6000) {
            result = result.substring(0, 5900) + "\n... [Diff truncated to stay within AI rate limits] ...\n";
        }

        log.info("Diff preprocessor run. Size reduced from {} to {} chars. (Optimized headers for {} files)",
                originalLength, result.length(), optimizedFilesCount);
        return result;

    }

    private static boolean isDeletedFile(String fileDiff) {
        return fileDiff.contains("deleted file mode") || 
               fileDiff.contains("+++ /dev/null");
    }

    private static boolean isLockFile(String fileDiff) {
        String[] lines = fileDiff.split("\\r?\\n");
        if (lines.length == 0) {
            return false;
        }
        String firstLine = lines[0];
        return firstLine.contains("/pnpm-lock.yaml") || 
               firstLine.contains("/package-lock.json") || 
               firstLine.contains("/yarn.lock") ||
               firstLine.contains("/cargo.lock") ||
               firstLine.contains("/Gemfile.lock") ||
               firstLine.contains("/poetry.lock") ||
               firstLine.contains("/composer.lock");
    }

    private static String cleanModifiedFileDiff(String fileDiff) {
        String[] lines = fileDiff.split("\\r?\\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            // Strip lines starting with "-" unless it is the file header indicator "---"
            if (line.startsWith("-") && !line.startsWith("---")) {
                continue;
            }
            cleaned.append(line).append("\n");
        }
        return cleaned.toString();
    }

    private static String extractHeader(String fileDiff) {
        String[] lines = fileDiff.split("\\r?\\n");
        StringBuilder header = new StringBuilder();
        for (String line : lines) {
            // Stop at the start of a hunk (diff hunk header starts with @@)
            if (line.startsWith("@@")) {
                break;
            }
            header.append(line).append("\n");
        }
        header.append("(File content stripped from review context)\n");
        return header.toString();
    }
}
