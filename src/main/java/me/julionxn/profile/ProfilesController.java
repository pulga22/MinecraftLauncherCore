package me.julionxn.profile;

import me.julionxn.CoreLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfilesController {

    private final CoreLogger logger;
    private final Path profilesPath;
    private final HashMap<String, Profile> loadedProfiles = new HashMap<>();

    public ProfilesController(CoreLogger logger, Path profilesPath){
        this.logger = logger;
        this.profilesPath = profilesPath;
    }

    public HashMap<String, Profile> getLoadedProfiles(){
        return loadedProfiles;
    }

    public void loadProfiles(){
        Set<Path> directories = getDirectories(profilesPath);
        for (Path directory : directories) {
            String id = directory.getFileName().toString();
            loadedProfiles.put(id, new Profile(id, directory));
        }
    }

    private Set<Path> getDirectories(Path path){
        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(Files::isDirectory)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Profile> getProfile(String id){
        if (loadedProfiles.containsKey(id)){
            return Optional.ofNullable(loadedProfiles.get(id));
        }
        Path profilePath = profilesPath.resolve(id);
        File profileFile = profilePath.toFile();
        if (!profileFile.exists() && !profileFile.mkdir()){
            logger.error("Error making profile " + id + " folder.");
            return Optional.empty();
        }
        Profile profile = new Profile(id, profilePath);
        loadedProfiles.put(id, profile);
        return Optional.of(profile);
    }

}
