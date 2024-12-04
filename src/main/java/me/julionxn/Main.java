package me.julionxn;

import me.julionxn.instance.MinecraftInstance;
import me.julionxn.instance.MinecraftOptions;
import me.julionxn.instance.PlayerInfo;
import me.julionxn.profiles.Profile;
import me.julionxn.versions.MinecraftVersion;
import me.julionxn.versions.loaders.FabricLoader;

import java.nio.file.Path;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {

        LauncherData launcherData = new LauncherData("JNLauncher",
                "1.0",
                Path.of("C:/"));
        Launcher launcher = new Launcher(launcherData);
        Optional<MinecraftVersion> versionOpt = launcher.getVersionsController()
                .installVersion("1.21.3",  new FabricLoader("0.16.9"),(status, progress) -> {

                });
        if (versionOpt.isEmpty()){
            System.err.println("Error installing version.");
            return;
        }
        MinecraftVersion version = versionOpt.get();
        Profile profile = launcher.getProfilesController().getProfile("testing");
        MinecraftOptions minecraftOptions = new MinecraftOptions();
        MinecraftInstance instance = new MinecraftInstance(launcher,
                version, minecraftOptions, profile,
                new PlayerInfo("pepe", "xd", "a"));
        instance.run();
    }
}