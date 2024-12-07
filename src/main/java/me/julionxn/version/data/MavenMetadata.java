package me.julionxn.version.data;

import java.util.List;

public record MavenMetadata(String release, String latest, List<String> versions) {
}
