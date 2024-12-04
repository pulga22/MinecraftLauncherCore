package me.julionxn.data;

import java.nio.file.Path;

public record TempFolder(Path path, Runnable close) {
}
