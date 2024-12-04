package me.julionxn.files;

import me.julionxn.CoreLogger;
import me.julionxn.LauncherData;

import java.io.File;
import java.nio.file.Path;

public class SystemController {

    private final CoreLogger logger;
    private final File rootFile;
    private final Path launcherFolderPath;
    private final File launcherFolderFile;
    private Natives natives;
    private String osName;

    public SystemController(CoreLogger logger, LauncherData launcherData){
        this.logger = logger;
        this.rootFile = launcherData.rootPath().toFile();
        this.launcherFolderPath = launcherData.rootPath().resolve(launcherData.launcherName());
        this.launcherFolderFile = launcherFolderPath.toFile();
    }

    public void initialize(){
        checkFolders();
        fetchOsAndNatives();
    }

    public void checkFolders(){
        if (!rootFile.exists()){
            logger.error("Root path does not exist.");
            return;
        }
        if (!launcherFolderFile.exists() && !launcherFolderFile.mkdir()){
            logger.error("Error making launcher folder.");
        }
        //cache folders
        checkCriticalFolder("cache");
        //logs folders
        checkCriticalFolder("logs");
        //data folders
        checkCriticalFolder("data");
        checkCriticalFolder("data/assets");
        checkCriticalFolder("data/assets/indexes");
        checkCriticalFolder("data/assets/objects");
        checkCriticalFolder("data/assets/skins");
        checkCriticalFolder("data/libraries");
        checkCriticalFolder("data/versions");
        checkCriticalFolder("data/natives");
        checkCriticalFolder("data/runtimes");
        checkCriticalFolder("data/temp");
        //profiles folders
        checkCriticalFolder("profiles");
    }

    private void checkCriticalFolder(String folder){
        File folderFile = launcherFolderPath.resolve(folder).toFile();
        if (!folderFile.exists() && !folderFile.mkdir()){
            logger.error("Error making " + folder + " folder.");
        }
    }

    private void fetchOsAndNatives() {
        String osNameProperty = System.getProperty("os.name").toLowerCase();
        String osArchProperty = System.getProperty("os.arch").toLowerCase();

        // Detect OS name
        if (osNameProperty.contains("win")) {
            osName = "windows";
        } else if (osNameProperty.contains("mac") || osNameProperty.contains("os x")) {
            osName = "osx";
        } else if (osNameProperty.contains("nix") || osNameProperty.contains("nux") || osNameProperty.contains("aix")) {
            osName = "linux";
        } else {
            osName = "none";
        }

        // Detect Natives
        switch (osName) {
            case "windows" -> {
                if (osArchProperty.contains("amd64") || osArchProperty.contains("x86_64")) {
                    natives = Natives.WIN64;  // Windows 64-bit (x86_64)
                } else if (osArchProperty.contains("arm") || osArchProperty.contains("aarch64")) {
                    natives = Natives.WIN_ARM64;  // Windows ARM 64-bit
                } else {
                    natives = Natives.WIN86;  // Windows 32-bit (x86)
                }
            }
            case "osx" -> {
                if (osArchProperty.contains("arm") || osArchProperty.contains("aarch64")) {
                    natives = Natives.OSX_ARM64;  // macOS ARM 64-bit
                } else {
                    natives = Natives.OSX;  // macOS (Intel-based)
                }
            }
            case "linux" -> {
                if (osArchProperty.contains("arm") || osArchProperty.contains("aarch64")) {
                    natives = Natives.LINUX_ARM;  // Linux ARM-based
                } else if (osArchProperty.contains("x86") || osArchProperty.contains("amd64") || osArchProperty.contains("x86_64")) {
                    natives = Natives.LINUX;  // Linux x86 or x86_64
                } else {
                    natives = Natives.LINUX_OTHER;  // Other Linux architectures
                }
            }
            default -> natives = Natives.NONE;  // Unsupported platform
        }
    }

    public Natives getNatives(){
        return natives;
    }

    public String getOsName(){
        return osName;
    }

}
