package io.github.julionxn.data;

import java.nio.file.Path;

public record TempFolder(Path path, Runnable close) {
}
