package io.kestra.core.runners;

import io.kestra.core.utils.IdUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Local filesystem implementation of the {@link WorkingDir} interface.
 * <p>
 * Each instance manages a single temporary directory on the local filesystem, identified by a
 * unique ID. The directory is created on demand (lazily) when {@link #path(boolean) path(true)} is
 * called, and can be entirely removed via {@link #cleanup()}.
 * <p>
 * This class is thread-safe with respect to directory creation: {@link #path(boolean)} is
 * {@code synchronized} to guarantee that at most one thread initialises the directory.
 */
public class LocalWorkingDir implements WorkingDir {

    private final Path workingDirPath;

    /**
     * Creates a new {@link LocalWorkingDir} with a randomly generated identifier.
     *
     * @param tmpdirBasePath the base temporary directory under which this working directory
     *                       will be created.
     */
    public LocalWorkingDir(final Path tmpdirBasePath) {
        this(tmpdirBasePath, IdUtils.create());
    }

    /**
     * Creates a new {@link LocalWorkingDir} with the supplied identifier.
     *
     * @param tmpdirBasePath the base temporary directory under which this working directory
     *                       will be created.
     * @param workingDirId   the identifier used as the sub-directory name inside
     *                       {@code tmpdirBasePath}.
     */
    public LocalWorkingDir(final Path tmpdirBasePath, final String workingDirId) {
        this.workingDirPath = tmpdirBasePath.resolve(workingDirId);
    }

    /** {@inheritDoc} */
    @Override
    public Path path() {
        return path(true);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Path path(final boolean create) {
        if (create && !this.workingDirPath.toFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.workingDirPath.toFile().mkdirs();
        }
        return this.workingDirPath;
    }

    /** {@inheritDoc} */
    @Override
    public Path resolve(final Path path) {
        if (path == null) {
            return path();
        }

        // Reject absolute paths (e.g., /etc/passwd, C:\Windows)
        if (path.isAbsolute()) {
            throw new IllegalArgumentException(
                "The path to resolve must be a relative path inside the current working directory.");
        }

        final String pathStr = path.toString();
        // Reject any segment containing ".." (e.g., "../escape", "sub/../../../etc", or "foo/..")
        if (pathStr.contains(".." + File.separator)
                || pathStr.contains(File.separator + "..")
                || pathStr.equals("..")) {
            throw new IllegalArgumentException(
                "The path to resolve must be a relative path inside the current working directory.");
        }

        final Path baseDir = path();
        final Path resolved = baseDir.resolve(path).toAbsolutePath();

        // Defense-in-depth: ensure the resolved path is still inside the base directory
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException(
                "The path to resolve must be a relative path inside the current working directory.");
        }

        return resolved;
    }

    /** {@inheritDoc} */
    @Override
    public Path createTempFile() throws IOException {
        return createTempFile(null, null);
    }

    /** {@inheritDoc} */
    @Override
    public Path createTempFile(final String extension) throws IOException {
        return createTempFile(null, extension);
    }

    /** {@inheritDoc} */
    @Override
    public Path createTempFile(final byte[] content) throws IOException {
        return createTempFile(content, null);
    }

    /** {@inheritDoc} */
    @Override
    public Path createTempFile(final byte[] content, final String extension) throws IOException {
        final String suffix =
            (extension != null && !extension.startsWith(".")) ? "." + extension : extension;
        final Path tempFile = Files.createTempFile(this.path(), null, suffix);
        if (content != null) {
            Files.write(tempFile, content);
        }
        return tempFile;
    }

    /** {@inheritDoc} */
    @Override
    public Path createFile(final String filename) throws IOException {
        return createFile(filename, (byte[]) null);
    }

    /** {@inheritDoc} */
    @Override
    public Path createFile(final String filename, final byte[] content) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                "Cannot create a working directory file with a null or empty name");
        }
        final Path newFilePath = this.resolve(Path.of(filename));
        Files.createDirectories(newFilePath.getParent());
        Files.createFile(newFilePath);
        if (content != null) {
            Files.write(newFilePath, content);
        }
        return newFilePath;
    }

    /** {@inheritDoc} */
    @Override
    public List<Path> findAllFilesMatching(final List<String> patterns) throws IOException {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }

        final Path basePath = path();
        final List<PathMatcher> matchers = new ArrayList<>(patterns.size());
        for (final String pattern : patterns) {
            final String matcherPattern;
            if (pattern.startsWith("glob:") || pattern.startsWith("regex:")) {
                matcherPattern = pattern;
            } else {
                matcherPattern = "glob:" + basePath + File.separator + pattern;
            }
            matchers.add(FileSystems.getDefault().getPathMatcher(matcherPattern));
        }

        final Set<Path> matched = new LinkedHashSet<>();
        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                                             final BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    // never follow symlinks
                    return FileVisitResult.CONTINUE;
                }
                for (final PathMatcher matcher : matchers) {
                    if (matcher.matches(file)) {
                        matched.add(file);
                        break;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return new ArrayList<>(matched);
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() throws IOException {
        if (workingDirPath != null && Files.exists(workingDirPath)) {
            FileUtils.deleteDirectory(workingDirPath.toFile());
        }
    }
}
