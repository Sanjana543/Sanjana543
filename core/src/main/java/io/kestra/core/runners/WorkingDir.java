package io.kestra.core.runners;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for a dedicated working directory associated with a task-run context.
 * <p>
 * A working directory is a local, temporary directory attached to the execution of a single task.
 * It provides utilities to create temporary files, resolve paths safely (with path-traversal
 * protection), discover files by glob/regex patterns, and clean up all resources when the task
 * completes.
 * <p>
 * A concrete implementation can be obtained via the runner context:
 *
 * @see RunContext#workingDir()
 */
public interface WorkingDir {

    /**
     * Returns the working directory path, creating the directory if it does not already exist.
     *
     * @return the {@link Path} of the working directory.
     */
    Path path();

    /**
     * Returns the working directory path, optionally creating the directory if it does not exist.
     *
     * @param create {@code true} if the directory should be created when it does not exist;
     *               {@code false} to return the path without creating the directory.
     * @return the {@link Path} of the working directory.
     */
    Path path(boolean create);

    /**
     * Resolves a relative path inside the working directory.
     * <p>
     * This method is null-friendly: passing {@code null} returns the working directory itself.
     * Absolute paths and any path that attempts to escape the working directory
     * (e.g., {@code ../../etc/passwd} or {@code subdir/../../../escape}) are rejected.
     *
     * @param path the relative path to resolve, or {@code null} to get the working directory.
     * @return the absolute resolved {@link Path} inside the working directory.
     * @throws IllegalArgumentException if {@code path} is absolute or escapes the working directory.
     */
    Path resolve(Path path);

    /**
     * Creates a new empty temporary file in the working directory.
     *
     * @return the {@link Path} of the created file.
     * @throws IOException if an error occurs while creating the file.
     */
    Path createTempFile() throws IOException;

    /**
     * Creates a new empty temporary file with the given extension in the working directory.
     *
     * @param extension the file extension (e.g., {@code ".ion"} or {@code "ion"});
     *                  may be {@code null}, in which case {@code ".tmp"} is used.
     * @return the {@link Path} of the created file.
     * @throws IOException if an error occurs while creating the file.
     */
    Path createTempFile(String extension) throws IOException;

    /**
     * Creates a new temporary file with the given content in the working directory.
     *
     * @param content the bytes to write into the file; may be {@code null} to create an empty file.
     * @return the {@link Path} of the created file.
     * @throws IOException if an error occurs while creating or writing the file.
     */
    Path createTempFile(byte[] content) throws IOException;

    /**
     * Creates a new temporary file with the given content and extension in the working directory.
     *
     * @param content   the bytes to write into the file; may be {@code null} to create an empty file.
     * @param extension the file extension (e.g., {@code ".ion"} or {@code "ion"});
     *                  may be {@code null}, in which case {@code ".tmp"} is used.
     * @return the {@link Path} of the created file.
     * @throws IOException if an error occurs while creating or writing the file.
     */
    Path createTempFile(byte[] content, String extension) throws IOException;

    /**
     * Creates an empty file at the given relative path inside the working directory.
     * <p>
     * Any intermediate directories are created automatically.
     *
     * @param filename the relative file name or path; must not be {@code null} or blank.
     * @return the {@link Path} of the created file.
     * @throws IOException              if an error occurs while creating the file.
     * @throws IllegalArgumentException if {@code filename} is {@code null} or blank.
     */
    Path createFile(String filename) throws IOException;

    /**
     * Creates a file at the given relative path with the supplied content inside the working directory.
     * <p>
     * Any intermediate directories are created automatically.
     *
     * @param filename the relative file name or path; must not be {@code null} or blank.
     * @param content  the bytes to write into the file; may be {@code null} to create an empty file.
     * @return the {@link Path} of the created file.
     * @throws IOException              if an error occurs while creating or writing the file.
     * @throws IllegalArgumentException if {@code filename} is {@code null} or blank.
     */
    Path createFile(String filename, byte[] content) throws IOException;

    /**
     * Finds all regular files in the working directory that match at least one of the given patterns.
     * <p>
     * Each pattern may be:
     * <ul>
     *   <li>a glob pattern prefixed with {@code "glob:"} — used as-is by the JDK {@link java.nio.file.PathMatcher};</li>
     *   <li>a regex pattern prefixed with {@code "regex:"} — used as-is by the JDK {@link java.nio.file.PathMatcher};</li>
     *   <li>a plain glob expression (no prefix) — automatically expanded to
     *       {@code "glob:<workingDirPath>/<pattern>"}.</li>
     * </ul>
     * A file appears at most once in the result even when it matches multiple patterns.
     * Symbolic links are never followed.
     *
     * @param patterns the list of patterns to match; {@code null} or empty returns an empty list.
     * @return the de-duplicated list of matched {@link Path}s.
     * @throws IOException if an error occurs while walking the directory tree.
     */
    List<Path> findAllFilesMatching(List<String> patterns) throws IOException;

    /**
     * Deletes the entire working directory and all of its contents.
     * <p>
     * After cleanup, calling {@link #path(boolean) path(true)} will recreate the directory at the
     * same path so the working directory can continue to be used.
     *
     * @throws IOException if an error occurs while deleting the directory.
     */
    void cleanup() throws IOException;
}
