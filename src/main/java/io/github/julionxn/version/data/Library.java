package io.github.julionxn.version.data;

import java.nio.file.Path;

public record Library(String artifact, String version, Path path) {
}
