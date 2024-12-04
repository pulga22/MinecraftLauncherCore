package me.julionxn.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.julionxn.LauncherData;
import me.julionxn.data.DataController;
import me.julionxn.files.Natives;
import me.julionxn.files.SystemController;
import me.julionxn.profiles.Profile;
import me.julionxn.versions.FetchingUtils;
import me.julionxn.versions.Library;
import me.julionxn.versions.MinecraftVersion;
import me.julionxn.versions.RuntimeComponentInfo;
import me.julionxn.versions.loaders.Loader;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ArgumentBuilder {

    private final SystemController systemController;
    private final DataController dataController;
    private final LauncherData launcherData;
    private final MinecraftVersion minecraftVersion;
    private final MinecraftOptions minecraftOptions;
    private final Profile profile;
    private final PlayerInfo playerInfo;

    public ArgumentBuilder(MinecraftInstance instance){
        this.systemController = instance.getSystemController();
        this.dataController = instance.getDataController();
        this.launcherData = instance.getLauncherData();
        this.minecraftVersion = instance.getMinecraftVersion();
        this.minecraftOptions = instance.getMinecraftOptions();
        this.profile = instance.getProfile();
        this.playerInfo = instance.getPlayerInfo();
    }

    public String buildCommand(){
        List<String> command = new ArrayList<>();
        JsonObject versionManifest = minecraftVersion.getVersionData();
        //Java Path
        RuntimeComponentInfo runtimeComponentInfo = minecraftVersion.getRuntimeComponentInfo();
        String component = runtimeComponentInfo.version();
        String javaPath = "\"" + dataController.getRuntimesPath().resolve(component) + "/bin/java\"";
        command.add(javaPath);
        //Parse JVM
        parseJVMRules(systemController, versionManifest, command);
        //Main class
        String mainClass = minecraftVersion.getMainClass();
        command.add(mainClass);
        //Parse GameArgs
        parseManifestGameArgs(versionManifest, command);
        //Parse libraries
        String classPathSeparator = FetchingUtils.getClassPathSeparator(systemController.getOsName());
        List<Library> allLibraries = fetchLibraries(versionManifest);
        String cp = parseLibraries(allLibraries, classPathSeparator);
        //Build command string
        StringBuilder builder = new StringBuilder();
        for (String string : command) {
            builder.append(string);
            builder.append(" ");
        }
        String cmd = builder.toString();
        //Replace args
        cmd = replaceArgs(cmd, cp, classPathSeparator);
        cmd = cmd.replace("\"", "");
        return cmd;
    }

    private String parseLibraries(List<Library> libraries, String classPathSeparator) {
        Map<String, List<Library>> groupedByArtifact = libraries.stream()
                .collect(Collectors.groupingBy(Library::artifact));
        List<Library> filteredLibraries = groupedByArtifact.values().stream()
                .flatMap(group -> {
                    if (group.size() == 1) {
                        return group.stream();
                    }
                    return group.stream()
                            .collect(Collectors.groupingBy(Library::version))
                            .entrySet().stream()
                            .max(Comparator.comparingInt(entry -> parseVersionToInt(entry.getKey())))
                            .map(Map.Entry::getValue)
                            .orElse(group)
                            .stream();
                })
                .toList();
        return filteredLibraries.stream()
                .map(library -> library.path().toString())
                .collect(Collectors.joining(classPathSeparator));
    }

    private int parseVersionToInt(String version) {
        try {
            return Integer.parseInt(
                    Arrays.stream(version.split("\\."))
                            .map(part -> String.format("%02d", Integer.parseInt(part)))
                            .collect(Collectors.joining())
            );
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /*private String parseLibraries(List<Library> libraries, String classPathSeparator) {
        Map<String, List<Library>> groupedByArtifact = libraries.stream()
                .collect(Collectors.groupingBy(Library::artifact));
        List<Library> filteredLibraries = new ArrayList<>();

        for (List<Library> group : groupedByArtifact.values()) {
            if (group.size() == 1){
                filteredLibraries.add(group.get(0));
                continue;
            }
            Map<String, List<Library>> groupedByVersion = group.stream()
                    .collect(Collectors.groupingBy(Library::version));
            Set<String> ketSet = groupedByVersion.keySet();
            String lastVersion = null;
            int higher = -1;
            for (String version : ketSet) {
                if (lastVersion == null){
                    lastVersion = version;
                    higher = parseVersionToInt(version);
                    continue;
                }
                int parsedVersion = parseVersionToInt(version);
                if (parsedVersion > higher) {
                    higher = parsedVersion;
                    lastVersion = version;
                }
            }
            List<Library> higherLibraries = groupedByVersion.get(lastVersion);
            filteredLibraries.addAll(higherLibraries);
        }
        StringBuilder builder = new StringBuilder();
        for (Library library : filteredLibraries) {
            builder.append(library.path());
            builder.append(classPathSeparator);
        }
        if (!builder.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private int parseVersionToInt(String version) {
        try {
            String[] parts = version.split("\\.");
            StringBuilder combined = new StringBuilder();
            for (String part : parts) {
                combined.append(String.format("%02d", Integer.parseInt(part)));
            }
            return Integer.parseInt(combined.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }*/

    private String replaceArgs(String cmd, String cp, String classPathSeparator){
        String version = minecraftVersion.getVersion();
        String assetIndex = minecraftVersion.getAssetIndexInfo().id();
        String nativesDirectory = dataController.getNativesPath().resolve(minecraftVersion.getVersion()).toString();
        String librariesDirectory = dataController.getLibrariesPath().toString();
        String assetsDirectory = dataController.getAssetsPath().toString();
        String resolutionWidth = minecraftOptions.resolutionWidth;
        String resolutionHeight = minecraftOptions.resolutionHeight;
        String userType = minecraftOptions.userType;
        String launcherName = launcherData.launcherName();
        String launcherVersion = launcherData.launcherVersion();
        String releaseType = minecraftVersion.getVersionType().toString().toLowerCase();
        String gameDirectory = profile.getProfilePath().toString();
        String username = playerInfo.username();
        String UUID = playerInfo.UUID();
        String token = playerInfo.token();

        cmd = cmd.replace("${classpath}", cp);
        cmd = cmd.replace("${natives_directory}", nativesDirectory);
        cmd = cmd.replace("${launcher_name}", launcherName);
        cmd = cmd.replace("${launcher_version}", launcherVersion);
        cmd = cmd.replace("${auth_player_name}", username);
        cmd = cmd.replace("${version_name}", version);
        cmd = cmd.replace("${game_directory}", gameDirectory);
        cmd = cmd.replace("${assets_root}", assetsDirectory);
        cmd = cmd.replace("${assets_index_name}", assetIndex);
        cmd = cmd.replace("${auth_uuid}", UUID);
        cmd = cmd.replace("${auth_access_token}", token);
        cmd = cmd.replace("${user_type}", userType);
        cmd = cmd.replace("${version_type}", releaseType);
        cmd = cmd.replace("${user_properties}", "{}");
        cmd = cmd.replace("${resolution_width}", resolutionWidth);
        cmd = cmd.replace("${resolution_height}", resolutionHeight);
        cmd = cmd.replace("${auth_session}", token);
        cmd = cmd.replace("${library_directory}", librariesDirectory);
        cmd = cmd.replace("${classpath_separator}", classPathSeparator);
        return cmd;
    }

    private List<Library> fetchLibraries(JsonObject manifest){
        JsonArray libraries = manifest.get("libraries").getAsJsonArray();
        String os = systemController.getOsName();
        Path librariesDirectory = dataController.getLibrariesPath();
        Path versionsDirectory = dataController.getVersionsPath();
        List<Library> vanillaLibraries = new ArrayList<>();
        Loader loader = minecraftVersion.getLoader();
        if (loader != null){
            List<Library> loaderLibraries = loader.getInstaller().getLibraries();
            vanillaLibraries.addAll(loaderLibraries);
        }
        for (JsonElement library : libraries) {
            if (library.getAsJsonObject().has("rules")){
                JsonArray rules = library.getAsJsonObject().getAsJsonArray("rules");
                boolean breakLibrary = false;
                for (JsonElement rule : rules) {
                    JsonObject osRule = rule.getAsJsonObject().getAsJsonObject("os");
                    if (osRule.has("name")){
                        String validOs = osRule.get("name").getAsString();
                        if (!validOs.equals(os)){
                            breakLibrary = true;
                        }
                    }
                }
                if (breakLibrary) continue;
            }
            JsonObject downloads = library.getAsJsonObject().get("downloads").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : downloads.entrySet()) {
                JsonObject value = entry.getValue().getAsJsonObject();
                String relativePath = value.get("path").getAsString();
                Path path = librariesDirectory.resolve(relativePath);
                String artifact = path.getParent().getParent().getFileName().toString();
                String version = path.getParent().getFileName().toString();
                vanillaLibraries.add(new Library(artifact, version, path));
            }
        }
        String jarVersion = minecraftVersion.getVersion();
        Path jarPath = versionsDirectory.resolve(jarVersion + "/" + jarVersion + ".jar");
        vanillaLibraries.add(new Library("minecraft-client", jarVersion, jarPath));
        return vanillaLibraries;
    }

    private void parseManifestGameArgs(JsonObject manifest, List<String> command){
        JsonObject arguments = manifest.get("arguments").getAsJsonObject();
        JsonArray game = arguments.get("game").getAsJsonArray();
        for (JsonElement jsonElement : game) {
            if (jsonElement.isJsonObject()){
                JsonObject arg = jsonElement.getAsJsonObject();
                JsonArray rules = arg.getAsJsonArray("rules");
                JsonElement values = arg.get("value");
                for (JsonElement ruleElement : rules) {
                    JsonObject rule = ruleElement.getAsJsonObject();
                    JsonObject features = rule.get("features").getAsJsonObject();
                    for (Map.Entry<String, JsonElement> stringJsonElementEntry : features.entrySet()) {
                        String key = stringJsonElementEntry.getKey();
                        boolean value = stringJsonElementEntry.getValue().getAsBoolean();
                        switch (key) {
                            case "has_custom_resolution" -> {
                                if (value && minecraftOptions.customResolution){
                                    for (JsonElement element : (JsonArray) values) {
                                        String val = element.getAsString();
                                        command.add(val);
                                    }
                                }
                            }
                        }
                    }
                }
                continue;
            }
            String arg = jsonElement.getAsString();
            command.add(arg);
        }
    }

    private void parseJVMRules(SystemController controller, JsonObject manifest, List<String> command){
        String os = controller.getOsName();
        Natives natives = controller.getNatives();
        command.add("-Dorg.lwjgl.util.Debug=true");
        command.add("-Dorg.lwjgl.util.DebugLoader=true");
        JsonArray jvmArgs = manifest.get("arguments").getAsJsonObject()
                .get("jvm").getAsJsonArray();
        for (JsonElement jvmArg : jvmArgs) {
            if (jvmArg.isJsonObject()){
                JsonObject ruleData = jvmArg.getAsJsonObject();
                JsonArray rules = ruleData.getAsJsonArray("rules");
                String arg = ruleData.get("value").getAsString();
                for (JsonElement jsonElement : rules) {
                    JsonObject rule = jsonElement.getAsJsonObject();
                    JsonObject osRule = rule.get("os").getAsJsonObject();
                    if (osRule.has("name")){
                        String validOs = osRule.get("name").getAsString();
                        if (validOs.equals(os)){
                            command.add(arg);
                        }
                    }
                    if (osRule.has("arch")){
                        String validArch = osRule.get("arch").getAsString();
                        if (validArch.equals("x86")){
                            if (natives == Natives.WIN86){
                                command.add(arg);
                            }
                        }
                    }
                }
            } else {
                String rule = jvmArg.toString();
                command.add(rule);
            }
        }
        Loader loader = minecraftVersion.getLoader();
        if (loader != null){
            List<String> jvmLoaderArgs = loader.getInstaller().getJVMArgs();
            command.addAll(jvmLoaderArgs);
        }
    }

}
