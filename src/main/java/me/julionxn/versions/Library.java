package me.julionxn.versions;

import java.nio.file.Path;

public record Library(String artifact, String version, Path path) {
}
