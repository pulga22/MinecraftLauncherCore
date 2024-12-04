package me.julionxn.instance;

import me.julionxn.CoreLogger;
import me.julionxn.Launcher;
import me.julionxn.LauncherData;
import me.julionxn.data.DataController;
import me.julionxn.files.SystemController;
import me.julionxn.profiles.Profile;
import me.julionxn.versions.MinecraftVersion;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MinecraftInstance {

    private final LauncherData launcherData;
    private final SystemController systemController;
    private final DataController dataController;
    private final MinecraftVersion minecraftVersion;
    private final MinecraftOptions minecraftOptions;
    private final Profile profile;
    private final PlayerInfo playerInfo;
    private final CoreLogger logger;

    public MinecraftInstance(Launcher launcher, MinecraftVersion version, MinecraftOptions options, Profile profile, PlayerInfo playerInfo){
        this.launcherData = launcher.getLauncherData();
        this.systemController = launcher.getSystemController();
        this.dataController = launcher.getDataController();
        this.minecraftVersion = version;
        this.minecraftOptions = options;
        this.profile = profile;
        this.playerInfo = playerInfo;
        this.logger = launcher.getLogger();
    }

    public void run(){
        ArgumentBuilder argumentBuilder = new ArgumentBuilder(this);
        String cmd = argumentBuilder.buildCommand();
        System.out.println(cmd);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd.split(" "));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitCode = process.waitFor();
            logger.info("Process exited with code: " + exitCode);
        } catch (Exception e) {
            logger.error("Something went wrong. ", e);
        }
    }

    public LauncherData getLauncherData(){
        return launcherData;
    }

    public SystemController getSystemController() {
        return systemController;
    }

    public MinecraftVersion getMinecraftVersion() {
        return minecraftVersion;
    }

    public MinecraftOptions getMinecraftOptions() {
        return minecraftOptions;
    }

    public Profile getProfile() {
        return profile;
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

    public DataController getDataController() {
        return dataController;
    }
}
