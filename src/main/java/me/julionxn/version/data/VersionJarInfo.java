package me.julionxn.version.data;

import java.net.URL;

public record VersionJarInfo(String sha1, int size, URL url) {
}
