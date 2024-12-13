package io.github.julionxn.profile;

import io.github.julionxn.instance.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class URLProfiles {

    private final List<URLProfile> profiles = new ArrayList<>();

    public void clearProfiles(){
        profiles.clear();
    }

    public void addProfile(URLProfile profile){
        profiles.add(profile);
    }

    public List<URLProfile> getAllProfiles() {
        return profiles;
    }

    public List<URLProfile> getValidProfiles(PlayerInfo playerInfo){
        UUID playerUUID = UUID.fromString(playerInfo.UUID());
        return profiles.stream()
                .filter(profile -> profile.validUUIDs().contains(playerUUID))
                .toList();
    }

}
