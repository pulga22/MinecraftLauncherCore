package me.julionxn;

import java.nio.file.Path;

public record LauncherData(String launcherName, String launcherVersion, Path rootPath) {

    public LauncherData(String launcherName, String launcherVersion, String rootPath){
        this(launcherName, launcherVersion, Path.of(rootPath));
    }

    public LauncherData(String launcherName, String launcherVersion) {
        this(launcherName, launcherVersion, Path.of(System.getProperty("user.home")));
    }

}
