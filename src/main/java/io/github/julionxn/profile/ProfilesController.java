package io.github.julionxn.profile;

import io.github.julionxn.CoreLogger;
import io.github.julionxn.data.DataController;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfilesController {

    private final CoreLogger logger;
    private final DataController dataController;
    private final HashMap<String, Profile> loadedProfiles = new HashMap<>();
    private final Path profilesPath;

    public ProfilesController(CoreLogger logger, DataController dataController, Path profilesPath){
        this.logger = logger;
        this.dataController = dataController;
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

    public boolean profileExists(String id){
        return loadedProfiles.containsKey(id);
    }

    public URLProfiles getAllProfilesFrom(String urlStr){
        return getValidProfilesFrom(urlStr, null);
    }

    public URLProfiles getValidProfilesFrom(String urlStr, @Nullable String uuid) {
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return getProfilesFrom(new ModpackBundlerFetcher(logger, url, uuid));
    }

    public URLProfiles getProfilesFrom(ProfilesFetcher fetcher){
        return fetcher.fetch(this, dataController, fetcher.url, fetcher.uuid);
    }

    public void addProfile(Profile profile){
        loadedProfiles.put(profile.getId(), profile);
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

    public Path getProfilesPath(){
        return profilesPath;
    }

}
