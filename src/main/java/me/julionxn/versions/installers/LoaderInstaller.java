package me.julionxn.versions.installers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.files.Natives;
import me.julionxn.versions.Library;
import me.julionxn.versions.MinecraftVersion;
import me.julionxn.versions.loaders.Loader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;

public abstract class LoaderInstaller extends Installer {

    public abstract boolean install(CoreLogger logger, Loader loader, MinecraftVersion minecraftVersion, DataController dataController, String osName, Natives natives, ProgressCallback callback);

    public abstract List<String> getJVMArgs();

    public abstract List<Library> getLibraries();


    protected void moveContents(Path source, Path destination){
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = destination.resolve(source.relativize(file));
                    Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = destination.resolve(source.relativize(dir));
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Optional<JsonObject> loadJson(Path path){
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            return Optional.ofNullable(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
