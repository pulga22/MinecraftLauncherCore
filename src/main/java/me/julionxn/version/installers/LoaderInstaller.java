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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public abstract class LoaderInstaller extends Installer {

    public abstract boolean install(CoreLogger logger, MinecraftVersion minecraftVersion, DataController dataController, String osName, Natives natives, ProgressCallback callback);

    @Nullable
    public abstract List<String> getJVMArgs();

    @Nullable
    public abstract List<Library> getLibraries();

    protected void moveContents(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetFile = destination.resolve(source.relativize(file));
                Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetDir = destination.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected JsonObject loadJson(Path path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileReader reader = new FileReader(path.toFile())) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }
}
