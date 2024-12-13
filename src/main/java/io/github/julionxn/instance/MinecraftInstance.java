package io.github.julionxn.instance;

import io.github.julionxn.CoreLogger;
import io.github.julionxn.Launcher;
import io.github.julionxn.LauncherData;
import io.github.julionxn.data.DataController;
import io.github.julionxn.profile.Profile;
import io.github.julionxn.system.SystemController;
import io.github.julionxn.version.MinecraftVersion;

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
        CommandBuilder commandBuilder = new CommandBuilder(this);
        String cmd = commandBuilder.build();
        logger.info(cmd);
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
