package io.github.julionxn.cache;

import io.github.julionxn.instance.PlayerInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public record UserInfo(PlayerInfo playerInfo, String skinUrl, long expirationTime, @Nullable Path headImage) {
}
