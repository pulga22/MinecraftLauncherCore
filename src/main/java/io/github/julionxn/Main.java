package io.github.julionxn;

import io.github.julionxn.instance.MinecraftInstance;
import io.github.julionxn.instance.MinecraftOptions;
import io.github.julionxn.instance.PlayerInfo;
import io.github.julionxn.profile.Profile;
import io.github.julionxn.profile.URLProfile;
import io.github.julionxn.profile.URLProfiles;
import io.github.julionxn.version.MinecraftVersion;

import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        //==================== WITH URL FETCHING ======================
        LauncherData launcherData = new LauncherData("JNLauncher", "1.0", "C:/");
        Launcher launcher = new Launcher(launcherData);
        launcher.start();
        URLProfiles profiles = launcher.getProfilesController().getValidProfilesFrom("http://localhost/Testing/", "xd");
        URLProfile urlProfile = profiles.getAllProfiles().get(0);
        Optional<Profile> profileOpt = urlProfile.tempProfile().save();
        if (profileOpt.isEmpty()) return;
        Profile profile = profileOpt.get();
        MinecraftOptions minecraftOptions = new MinecraftOptions();
        MinecraftVersion version = urlProfile.minecraftVersion();
        launcher.getVersionsController().installVersion(version, (status, progress) -> {

        });
        MinecraftInstance instance = new MinecraftInstance(launcher, version, minecraftOptions, profile, new PlayerInfo("pepe", "xd", "a"));
        instance.run();


        /*
        // ==================== WITHOUT URL FETCHING ======================
        LauncherData launcherData = new LauncherData("JNLauncher", "1.0");
        Launcher launcher = new Launcher(launcherData);
        launcher.start();
        Optional<MinecraftVersion > versionOpt = launcher.getVersionsController()
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
        instance.run();*/


    }
}