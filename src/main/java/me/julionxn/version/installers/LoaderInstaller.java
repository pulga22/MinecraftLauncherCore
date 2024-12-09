package me.julionxn.version.installers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.system.Natives;
import me.julionxn.version.MinecraftVersion;
import me.julionxn.version.data.Library;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public abstract class LoaderInstaller extends Installer {

    public abstract boolean install(CoreLogger logger, MinecraftVersion minecraftVersion, DataController dataController, String osName, Natives natives, ProgressCallback callback);

    @Nullable
    public abstract List<String> getJVMArgs();

    @Nullable
    public abstract List<Library> getLibraries();

    protected JsonObject loadJson(Path path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileReader reader = new FileReader(path.toFile())) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }
}
