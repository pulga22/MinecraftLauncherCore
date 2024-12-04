package me.julionxn.versions;

import java.util.List;

public record MavenMetadata(String release, String latest, List<String> versions) {
}
