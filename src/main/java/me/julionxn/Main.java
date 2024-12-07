package me.julionxn;

import me.julionxn.instance.MinecraftInstance;
import me.julionxn.instance.MinecraftOptions;
import me.julionxn.instance.PlayerInfo;
import me.julionxn.profile.Profile;
import me.julionxn.version.MinecraftVersion;
import me.julionxn.version.loaders.FabricLoader;

import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        LauncherData launcherData = new LauncherData("JNLauncher", "1.0");
        Launcher launcher = new Launcher(launcherData);
        launcher.start();
        Optional<MinecraftVersion> versionOpt = launcher.getVersionsController()
                .installVersion("1.21.3", new FabricLoader("0.16.9"), (status, progress) -> {
                    //empty
                });
        if (versionOpt.isEmpty()){
            System.err.println("Error installing version.");
            return;
        }
        MinecraftVersion version = versionOpt.get();
        Optional<Profile> profileOpt = launcher.getProfilesController().getProfile("testing");
        if (profileOpt.isEmpty()){
            System.err.println("Error installing profile.");
            return;
        }
        Profile profile = profileOpt.get();
        MinecraftOptions minecraftOptions = new MinecraftOptions();
        MinecraftInstance instance = new MinecraftInstance(launcher,
                version, minecraftOptions, profile,
                new PlayerInfo("pepe", "xd", "a"));
        instance.run();
    }
}