package com.codeguardian.domain.model;

/**
 * Domain model representing a chunk of source code indexed in the vector store.
 */
public class CodeSnippet {
    private final String filePath;
    private final int startLine;
    private final int endLine;
    private final String content;

    public CodeSnippet(String filePath, int startLine, int endLine, String content) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
    }

    public String getFilePath() { return filePath; }
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return "CodeSnippet{" +
                "filePath='" + filePath + '\'' +
                ", startLine=" + startLine +
                ", endLine=" + endLine +
                '}';
    }
}
