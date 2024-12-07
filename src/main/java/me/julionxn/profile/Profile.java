package me.julionxn.profile;

import java.nio.file.Path;

public class Profile {

    private final String id;
    private final Path profilePath;
    private String description;
    private Path iconPath;

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

    public void setIconPath(Path iconPath){
        this.iconPath = iconPath;
    }

    public Path getIconPath(){
        return iconPath;
    }

    public String getId() {
        return id;
    }
}
