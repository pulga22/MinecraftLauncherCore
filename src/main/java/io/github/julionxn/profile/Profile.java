package io.github.julionxn.profile;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class Profile {

    private final String id;
    private final Path profilePath;
    private String description;
    @Nullable private Path iconPath;

    public Profile(String id, Path profilePath){
        this.id = id;
        this.profilePath = profilePath;
    }

    public Path getProfilePath(){
        return profilePath;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    public void setIconPath(@Nullable Path iconPath){
        this.iconPath = iconPath;
    }

    public @Nullable Path getIconPath(){
        return iconPath;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id='" + id + '\'' +
                ", profilePath=" + profilePath +
                ", description='" + description + '\'' +
                ", iconPath=" + iconPath +
                '}';
    }
}
