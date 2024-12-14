package io.github.julionxn.cache;

import com.google.gson.JsonObject;
import io.github.julionxn.CoreLogger;
import io.github.julionxn.instance.PlayerInfo;
import io.github.julionxn.utils.FetchingUtils;
import io.github.julionxn.utils.FilesUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class CacheController {

    private final CoreLogger logger;
    private final Path cacheFolder;
    private final File userFile;
    private @Nullable UserInfo userInfo;

    public CacheController(CoreLogger logger, Path cacheFolder){
        this.logger = logger;
        this.cacheFolder = cacheFolder;
        this.userFile = cacheFolder.resolve("user").toFile();
    }

    public void initialize(){
        Optional<UserInfo> userInfo = fetchUserInfo();
        userInfo.ifPresent(info -> this.userInfo = info);
    }

    public Optional<UserInfo> getUserInfo(){
        return Optional.ofNullable(userInfo);
    }

    public void clearUserInfo(){
        this.userInfo = null;
        try {
            FilesUtils.removeFile(userFile);
        } catch (IOException e) {
            logger.error("Error removing User Info: ", e);
        }
    }

    public void storeUserInfo(UserInfo userInfo){
        this.userInfo = userInfo;
        saveUserInfo();
    }

    public void saveUserInfo(){
        if (this.userInfo == null) return;
        JsonObject userJson = new JsonObject();
        PlayerInfo playerInfo = userInfo.playerInfo();
        userJson.addProperty("username", playerInfo.username());
        userJson.addProperty("uuid", playerInfo.UUID());
        userJson.addProperty("token", playerInfo.token());
        userJson.addProperty("skinUrl", userInfo.skinUrl());
        userJson.addProperty("expiration", userInfo.expirationTime());
        FetchingUtils.saveJson(userFile, userJson);
    }

    private Optional<UserInfo> fetchUserInfo(){
        if (!userFile.exists()) return Optional.empty();
        JsonObject userData;
        try {
            userData = FetchingUtils.loadJson(userFile.toPath());
            logger.info("User info fetched.");
        } catch (IOException e) {
            logger.error("Failed to fetch user info.");
            return Optional.empty();
        }
        String username = userData.get("username").getAsString();
        String uuid = userData.get("uuid").getAsString();
        String token = userData.get("token").getAsString();
        String skinUrl = userData.get("skinUrl").getAsString();
        long expirationTime = userData.get("expiration").getAsLong();
        PlayerInfo playerInfo = new PlayerInfo(username, uuid, token);
        UserInfo userInfo = new UserInfo(playerInfo, skinUrl, expirationTime);
        return Optional.of(userInfo);
    }

    public Path getCacheFolder(){
        return cacheFolder;
    }


}
