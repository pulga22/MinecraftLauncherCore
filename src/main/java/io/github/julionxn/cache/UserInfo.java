package io.github.julionxn.cache;

import io.github.julionxn.instance.PlayerInfo;

public record UserInfo(PlayerInfo playerInfo, String skinUrl, long expirationTime) {
}
