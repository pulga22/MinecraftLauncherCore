package me.julionxn.profile;

import me.julionxn.version.MinecraftVersion;

import java.util.List;
import java.util.UUID;

public record URLProfile(MinecraftVersion minecraftVersion, TempProfile tempProfile, List<UUID> validUUIDs) {
}
