package me.julionxn.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.files.Natives;
import me.julionxn.files.SystemController;
import me.julionxn.versions.loaders.Loader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class MinecraftVersion {

    private static final String VERSIONS_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String RUNTIMES_MANIFEST_URL = "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
    private final String version;
    private final Loader loader;
    private VersionType versionType;
    private JsonObject versionData;
    private AssetIndexInfo assetIndexInfo;
    private JsonArray libraries;
    private VersionJarInfo versionJarInfo;
    private RuntimeComponentInfo runtimeComponentInfo;
    private String mainClass;

    public MinecraftVersion(String version, Loader loader){
        this.version = version;
        this.loader = loader;
    }

    public void setMainClass(String mainClass){
        this.mainClass = mainClass;
    }

    public String getMainClass(){
        return mainClass;
    }

    public boolean loadMetadata(CoreLogger logger, SystemController systemController, ProgressCallback callback){
        String status = "Loading Version Metadata";
        callback.onProgress(status, 0);
        //Version manifest
        Optional<String> versionManifestUrlOpt = fetchVersionManifestUrl(logger);
        if (versionManifestUrlOpt.isEmpty()) return false;
        String versionManifestUrl = versionManifestUrlOpt.get();
        callback.onProgress(status, 1/6f);
        //Version data
        Optional<JsonObject> versionDataOpt = fetchVersionData(logger, versionManifestUrl);
        if (versionDataOpt.isEmpty()) return false;
        this.versionData = versionDataOpt.get();
        setMainClass(versionData.get("mainClass").getAsString());
        callback.onProgress(status, 2/6f);
        //Asset Index Info
        Optional<AssetIndexInfo> infoOpt = fetchAssetIndexInfo(logger, this.versionData);
        if (infoOpt.isEmpty()) return false;
        this.assetIndexInfo = infoOpt.get();
        callback.onProgress(status, 3/6f);
        //Libraries
        this.libraries = this.versionData.getAsJsonArray("libraries");
        callback.onProgress(status, 4/6f);
        //Version Jar Info
        Optional<VersionJarInfo> jarInfoOpt = fetchVersionJarInfo(logger, this.versionData);
        if (jarInfoOpt.isEmpty()) return false;
        this.versionJarInfo = jarInfoOpt.get();
        callback.onProgress(status, 5/6f);
        //Runtime Component Info
        Optional<RuntimeComponentInfo> runtimeInfoOpt = fetchRuntimeComponentInfo(logger, this.versionData, systemController.getNatives());
        if (runtimeInfoOpt.isEmpty()) return false;
        this.runtimeComponentInfo = runtimeInfoOpt.get();
        callback.onProgress(status, 1);
        return true;
    }

    private Optional<String> fetchVersionManifestUrl(CoreLogger logger){
        Optional<URL> versionsManifestOpt = FetchingUtils.getURL(VERSIONS_MANIFEST_URL);
        if (versionsManifestOpt.isEmpty()) {
            logger.error("Error trying to get URL from MalformedURL: " + VERSIONS_MANIFEST_URL);
            return Optional.empty();
        }
        URL versionManifestUrl = versionsManifestOpt.get();
        JsonObject versionManifestData;
        try {
            Optional<JsonObject> versionManifestDataOpt = FetchingUtils.fetchJsonData(versionManifestUrl);
            if (versionManifestDataOpt.isEmpty()) {
                logger.error("Bad RespondeCode from " + versionManifestUrl);
                return Optional.empty();
            }
            versionManifestData = versionManifestDataOpt.get();
        } catch (IOException e) {
            logger.error("Error loading Json RuntimesData from " + versionManifestUrl, e);
            return Optional.empty();
        }
        JsonArray versions = versionManifestData.getAsJsonArray("versions");
        for (JsonElement jsonElement : versions) {
            JsonObject versionData = jsonElement.getAsJsonObject();
            String versionId = versionData.get("id").getAsString();
            if (!versionId.equals(this.version)) continue;
            String url = versionData.get("url").getAsString();
            String type = versionData.get("type").getAsString();
            if (type.equals("snapshot")) {
                versionType = VersionType.SNAPSHOT;
            } else {
                versionType = VersionType.RELEASE;
            }
            return Optional.of(url);
        }
        return Optional.empty();
    }

    private Optional<JsonObject> fetchVersionData(CoreLogger logger, String versionManifestUrl){
        Optional<URL> versionUrlOpt = FetchingUtils.getURL(versionManifestUrl);
        if (versionUrlOpt.isEmpty()) {
            logger.error("Error trying to get URL from MalformedURL: " + versionManifestUrl);
            return Optional.empty();
        }
        URL versionUrl = versionUrlOpt.get();
        try {
            Optional<JsonObject> versionDataOpt = FetchingUtils.fetchJsonData(versionUrl);
            if (versionDataOpt.isEmpty()) {
                logger.error("Bad RespondeCode from " + versionUrl);
                return Optional.empty();
            }
            JsonObject versionData = versionDataOpt.get();
            return Optional.of(versionData);
        } catch (IOException e) {
            logger.error("Error loading Json VersionData from " + versionUrl, e);
            return Optional.empty();
        }
    }

    private Optional<AssetIndexInfo> fetchAssetIndexInfo(CoreLogger logger, JsonObject versionData){
        JsonObject assetInfo = versionData.getAsJsonObject("assetIndex");
        String id = assetInfo.get("id").getAsString();
        String indexUrl = assetInfo.get("url").getAsString();
        Optional<URL> assetIndexUrlOpt = FetchingUtils.getURL(indexUrl);
        if (assetIndexUrlOpt.isEmpty()) {
            logger.error("Error trying to get URL from MalformedURL: " + indexUrl);
            return Optional.empty();
        }
        URL assetIndexUrl = assetIndexUrlOpt.get();
        try {
            Optional<JsonObject> assetIndexDataOpt = FetchingUtils.fetchJsonData(assetIndexUrl);
            if (assetIndexDataOpt.isEmpty()) {
                logger.error("Bad RespondeCode from " + assetIndexUrl);
                return Optional.empty();
            }
            JsonObject assetIndexData = assetIndexDataOpt.get();
            AssetIndexInfo info = new AssetIndexInfo(id, assetIndexData);
            return Optional.of(info);
        } catch (IOException e) {
            logger.error("Error loading Json AssetIndexData from " + assetIndexUrl, e);
            return Optional.empty();
        }
    }

    private Optional<VersionJarInfo> fetchVersionJarInfo(CoreLogger logger, JsonObject versionData){
        JsonObject downloads = versionData.getAsJsonObject("downloads");
        JsonObject client  = downloads.getAsJsonObject("client");
        String sha1 = client.get("sha1").getAsString();
        int size = client.get("size").getAsInt();
        String clientJarUrlStr = client.get("url").getAsString();
        Optional<URL> clientJarUrlOpt = FetchingUtils.getURL(clientJarUrlStr);
        if (clientJarUrlOpt.isEmpty()) {
            logger.error("Error trying to get URL from MalformedURL: " + clientJarUrlStr);
            return Optional.empty();
        }
        URL clientJarUrl = clientJarUrlOpt.get();
        VersionJarInfo info = new VersionJarInfo(sha1, size, clientJarUrl);
        return Optional.of(info);
    }

    private Optional<RuntimeComponentInfo> fetchRuntimeComponentInfo(CoreLogger logger, JsonObject versionData, Natives natives){
        JsonObject javaVersion = versionData.getAsJsonObject("javaVersion");
        String componentVersion = javaVersion.get("component").getAsString();
        Optional<URL> runtimesUrlOpt = FetchingUtils.getURL(RUNTIMES_MANIFEST_URL);
        if (runtimesUrlOpt.isEmpty()) {
            logger.error("Error trying to get URL from MalformedURL: " + RUNTIMES_MANIFEST_URL);
            return Optional.empty();
        }
        URL runtimesUrl = runtimesUrlOpt.get();
        JsonObject runtimesData;
        try {
            Optional<JsonObject> runtimesDataOpt = FetchingUtils.fetchJsonData(runtimesUrl);
            if (runtimesDataOpt.isEmpty()) {
                logger.error("Bad RespondeCode from " + runtimesUrl);
                return Optional.empty();
            }
            runtimesData = runtimesDataOpt.get();
        } catch (IOException e) {
            logger.error("Error loading Json RuntimesData from " + runtimesUrl, e);
            return Optional.empty();
        }
        String nativesString = Natives.getNativesString(natives);
        JsonObject archComponents = runtimesData.getAsJsonObject(nativesString);
        JsonObject component = archComponents.getAsJsonArray(componentVersion).get(0).getAsJsonObject();
        JsonObject componentManifest = component.getAsJsonObject("manifest");
        String componentUrlStr = componentManifest.get("url").getAsString();
        URL componentURL;
        try {
            componentURL = new URL(componentUrlStr);
        } catch (MalformedURLException e) {
            logger.error("Error trying to get URL from MalformedURL: " + componentUrlStr);
            return Optional.empty();
        }
        JsonObject componentData;
        try {
            Optional<JsonObject> componentDataOpt = FetchingUtils.fetchJsonData(componentURL);
            if (componentDataOpt.isEmpty()) {
                logger.error("Bad RespondeCode from " + componentURL);
                return Optional.empty();
            }
            componentData = componentDataOpt.get();
        } catch (IOException e) {
            logger.error("Error loading Json ComponentData from " + runtimesUrl, e);
            return Optional.empty();
        }
        RuntimeComponentInfo info = new RuntimeComponentInfo(componentVersion, componentData);
        return Optional.of(info);
    }

    public String getVersion(){
        return version;
    }

    public VersionType getVersionType(){
        return versionType;
    }

    public JsonObject getVersionData(){
        return versionData;
    }

    public AssetIndexInfo getAssetIndexInfo() {
        return assetIndexInfo;
    }

    public JsonArray getLibraries() {
        return libraries;
    }

    public VersionJarInfo getVersionJarInfo() {
        return versionJarInfo;
    }

    public RuntimeComponentInfo getRuntimeComponentInfo() {
        return runtimeComponentInfo;
    }

    public Loader getLoader() {
        return loader;
    }

}
