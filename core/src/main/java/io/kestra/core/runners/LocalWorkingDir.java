package io.kestra.core.runners;

import io.kestra.core.utils.IdUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Local {@link WorkingDir} implementation backed by a directory on the local filesystem.
 */
public class LocalWorkingDir implements WorkingDir {
    private static final String INVALID_RESOLVE_MESSAGE =
        "The path to resolve must be a relative path inside the current working directory.";
    private static final String INVALID_FILENAME_MESSAGE =
        "Cannot create a working directory file with a null or empty name";

    private final Path workingDirPath;

    public LocalWorkingDir(Path tmpdirBasePath) {
        this(tmpdirBasePath, IdUtils.create());
    }

    public LocalWorkingDir(Path tmpdirBasePath, String workingDirId) {
        this.workingDirPath = Objects.requireNonNull(tmpdirBasePath, "tmpdirBasePath cannot be null").resolve(workingDirId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path path() {
        return this.path(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Path path(boolean create) {
        if (create) {
            File directory = this.workingDirPath.toFile();
            if (!directory.mkdirs() && !directory.isDirectory()) {
                throw new IllegalStateException("Unable to create working directory: " + this.workingDirPath);
            }
        }

        return this.workingDirPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(Path path) {
        if (path == null) {
            return this.path();
        }

        if (path.isAbsolute() || path.toString().contains(".." + File.separator)) {
            throw new IllegalArgumentException(INVALID_RESOLVE_MESSAGE);
        }

        Path basePath = this.path().toAbsolutePath().normalize();
        Path resolvedPath = basePath.resolve(path).normalize().toAbsolutePath();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException(INVALID_RESOLVE_MESSAGE);
        }

        return resolvedPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createTempFile() throws IOException {
        return this.createTempFile(null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createTempFile(String extension) throws IOException {
        return this.createTempFile(null, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createTempFile(byte[] content) throws IOException {
        return this.createTempFile(content, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createTempFile(byte[] content, String extension) throws IOException {
        String suffix = extension;
        if (suffix != null && !suffix.isBlank() && !suffix.startsWith(".")) {
            suffix = "." + suffix;
        }

        Path tempFile = Files.createTempFile(this.path(), null, suffix);

        if (content != null) {
            Files.write(tempFile, content);
        }

        return tempFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createFile(String filename) throws IOException {
        return this.createFile(filename, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createFile(String filename, byte[] content) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(INVALID_FILENAME_MESSAGE);
        }

        Path filePath = this.resolve(Path.of(filename));
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.createFile(filePath);

        if (content != null) {
            Files.write(filePath, content);
        }

        return filePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Path> findAllFilesMatching(List<String> patterns) throws IOException {
        if (patterns == null || patterns.isEmpty()) {
            return List.of();
        }

        Path basePath = this.path(false);
        if (!Files.exists(basePath)) {
            return List.of();
        }

        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }

            String matcherPattern = pattern.startsWith("glob:") || pattern.startsWith("regex:")
                ? pattern
                : "glob:" + basePath + File.separator + pattern;
            matchers.add(basePath.getFileSystem().getPathMatcher(matcherPattern));
        }

        if (matchers.isEmpty()) {
            return List.of();
        }

        Set<Path> matches = new LinkedHashSet<>();
        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }

                for (PathMatcher matcher : matchers) {
                    if (matcher.matches(file)) {
                        matches.add(file);
                        break;
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, visitor);

        return List.copyOf(matches);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() throws IOException {
        Path basePath = this.path(false);
        if (Files.exists(basePath)) {
            FileUtils.deleteDirectory(basePath.toFile());
        }
    }
}
