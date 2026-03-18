package io.kestra.core.runners;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for a dedicated working directory associated with a task-run context.
 *
 * @see RunContext#workingDir()
 */
public interface WorkingDir {
    /**
     * Returns the working directory path, creating it when needed.
     *
     * @return the working directory path
     */
    Path path();

    /**
     * Returns the working directory path and optionally creates it.
     *
     * @param create whether the working directory should be created when it does not exist
     * @return the working directory path
     */
    Path path(boolean create);

    /**
     * Resolves a relative path under the working directory.
     *
     * @param path the relative path to resolve, or {@code null} to resolve the working directory itself
     * @return the resolved path inside the working directory
     * @throws IllegalArgumentException if the provided path is absolute or escapes the working directory
     */
    Path resolve(Path path);

    /**
     * Creates an empty temporary file in the working directory.
     *
     * @return the created temporary file path
     * @throws IOException if the file cannot be created
     */
    Path createTempFile() throws IOException;

    /**
     * Creates an empty temporary file in the working directory using the provided extension.
     *
     * @param extension the desired file extension
     * @return the created temporary file path
     * @throws IOException if the file cannot be created
     */
    Path createTempFile(String extension) throws IOException;

    /**
     * Creates a temporary file in the working directory and writes the provided content to it.
     *
     * @param content the content to write
     * @return the created temporary file path
     * @throws IOException if the file cannot be created
     */
    Path createTempFile(byte[] content) throws IOException;

    /**
     * Creates a temporary file in the working directory, optionally using the provided extension,
     * and writes the provided content to it.
     *
     * @param content the content to write
     * @param extension the desired file extension
     * @return the created temporary file path
     * @throws IOException if the file cannot be created
     */
    Path createTempFile(byte[] content, String extension) throws IOException;

    /**
     * Creates a file at the given relative path inside the working directory.
     *
     * @param filename the relative file path to create
     * @return the created file path
     * @throws IOException if the file cannot be created
     * @throws IllegalArgumentException if the filename is {@code null} or blank
     */
    Path createFile(String filename) throws IOException;

    /**
     * Creates a file at the given relative path inside the working directory and writes the provided content to it.
     *
     * @param filename the relative file path to create
     * @param content the content to write
     * @return the created file path
     * @throws IOException if the file cannot be created
     * @throws IllegalArgumentException if the filename is {@code null} or blank
     */
    Path createFile(String filename, byte[] content) throws IOException;

    /**
     * Finds all regular files in the working directory that match at least one provided pattern.
     *
     * @param patterns the glob or regex patterns to match
     * @return the list of matching files
     * @throws IOException if file traversal fails
     */
    List<Path> findAllFilesMatching(List<String> patterns) throws IOException;

    /**
     * Cleans up the working directory and its contents.
     *
     * @throws IOException if cleanup fails
     */
    void cleanup() throws IOException;
}
