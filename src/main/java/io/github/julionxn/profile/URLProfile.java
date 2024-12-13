package io.github.julionxn.profile;

import io.github.julionxn.version.MinecraftVersion;

import java.util.List;
import java.util.UUID;

public record URLProfile(MinecraftVersion minecraftVersion, TempProfile tempProfile, List<UUID> validUUIDs) {
}
