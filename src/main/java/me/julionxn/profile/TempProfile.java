package me.julionxn.profile;

import me.julionxn.data.TempFolder;
import me.julionxn.utils.FilesUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class TempProfile {

    private final String profileName;
    @Nullable private final Path imagePath;
    private final String description;
    private final TempFolder tempFolder;
    private final ProfilesController profilesController;
    private boolean canBeSaved = true;

    public TempProfile(String profileName, TempFolder tempFolder, @Nullable Path imagePath, String description, ProfilesController profilesController) {
        this.profileName = profileName;
        this.imagePath = imagePath;
        this.description = description;
        this.tempFolder = tempFolder;
        this.profilesController = profilesController;
    }

    public Optional<Profile> save(){
        if (!canBeSaved) throw new RuntimeException("Current TempProfile has been removed previously");
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
        remove();
        return Optional.of(profile);
    }

    public void remove(){
        tempFolder.close().run();
        canBeSaved = false;
    }



}
