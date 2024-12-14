package io.github.julionxn.profile;

import io.github.julionxn.data.TempFolder;
import io.github.julionxn.utils.FilesUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class TempProfile {

    private final String profileName;
    @Nullable private final Path tempImagePath;
    @Nullable private final Path imagePath;
    private final String description;
    private final TempFolder tempFolder;
    private final ProfilesController profilesController;
    private boolean canBeSaved = true;

    public TempProfile(String profileName, TempFolder tempFolder, @Nullable Path tempImagePath, @Nullable Path imagePath, String description, ProfilesController profilesController) {
        this.profileName = profileName;
        this.tempImagePath = tempImagePath;
        this.imagePath = imagePath;
        this.description = description;
        this.tempFolder = tempFolder;
        this.profilesController = profilesController;
    }

    public String getProfileName(){
        return profileName;
    }

    public String getDescription(){
        return description;
    }

    public Optional<Path> getTempImagePath(){
        return Optional.ofNullable(tempImagePath);
    }

    public Optional<Profile> save(){
        if (!canBeSaved) throw new RuntimeException("Current TempProfile has been removed previously");
        if (profilesController.profileExists(profileName)) {
            //todo: check hash of currentProfile to check if should overwrite
            return profilesController.getProfile(profileName);
        }
        Optional<Profile> profileOpt = profilesController.getProfile(profileName);
        if (profileOpt.isEmpty()) return Optional.empty();
        Profile profile = profileOpt.get();
        profile.setDescription(description);
        profile.setIconPath(imagePath);
        Path destPath = profile.getProfilePath();
        Path currentPath = tempFolder.path().resolve(profileName);
        try {
            FilesUtils.moveContents(currentPath, destPath);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        profilesController.addProfile(profile);
        remove();
        return Optional.of(profile);
    }

    public void remove(){
        tempFolder.close().run();
        canBeSaved = false;
    }



}
