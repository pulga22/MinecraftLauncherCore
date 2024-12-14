package io.github.julionxn.profile;

import io.github.julionxn.data.TempFolder;
import io.github.julionxn.utils.FilesUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

public class TempProfile {

    private final String profileName;
    @Nullable private final Path tempImagePath;
    @Nullable private final Path imagePath;
    private final String description;
    private final CheckData checkData;
    private final TempFolder tempFolder;
    private final ProfilesController profilesController;
    private boolean canBeSaved = true;

    public TempProfile(String profileName, TempFolder tempFolder, @Nullable Path tempImagePath, @Nullable Path imagePath, String description, CheckData checkData, ProfilesController profilesController) {
        this.profileName = profileName;
        this.tempImagePath = tempImagePath;
        this.imagePath = imagePath;
        this.description = description;
        this.checkData = checkData;
        this.tempFolder = tempFolder;
        this.profilesController = profilesController;
    }

    public String getProfileName(){
        return profileName;
    }

    public String getDescription(){
        return description;
    }

    public Optional<Path> getTempImagePath(){
        return Optional.ofNullable(tempImagePath);
    }

    public Optional<Profile> save(){
        if (!canBeSaved) throw new RuntimeException("Current TempProfile has been removed previously");
        if (profilesController.profileExists(profileName)) {
            Optional<Profile> profileOpt = profilesController.getProfile(profileName);
            if (profileOpt.isEmpty()) return Optional.empty();
            Profile profile = profileOpt.get();
            Path profilePath = profile.getProfilePath();
            String currentHash;
            try {
                currentHash = generateFolderHash(profilePath.toFile(), checkData.files());
            } catch (IOException | NoSuchAlgorithmException e) {
                return Optional.empty();
            }
            if (currentHash.equals(checkData.hash())) return Optional.of(profile);
            try {
                FilesUtils.deleteSelectedFiles(profilePath, checkData.files());
                profilePath.toFile().mkdir();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Optional<Profile> profileOpt = profilesController.getProfile(profileName);
        if (profileOpt.isEmpty()) return Optional.empty();
        Profile profile = profileOpt.get();
        profile.setDescription(description);
        profile.setIconPath(imagePath);
        Path destPath = profile.getProfilePath();
        Path currentPath = tempFolder.path().resolve(profileName);
        try {
            FilesUtils.moveContents(currentPath, destPath);
        } catch (IOException e) {
            return Optional.empty();
        }
        profilesController.addProfile(profile);
        remove();
        return Optional.of(profile);
    }

    public void remove(){
        tempFolder.close().run();
        canBeSaved = false;
    }

    public String generateFolderHash(File folder, List<String> toCheck) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        updateDigestWithFolder(digest, folder, folder.getPath(), toCheck);
        return bytesToHex(digest.digest());
    }

    private void updateDigestWithFolder(MessageDigest digest, File folder, String rootPath, List<String> toCheck) throws IOException, NoSuchAlgorithmException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String relativePath = file.getPath().substring(rootPath.length()).replace("\\", "/");
                if (toCheck.contains(relativePath)) {
                    if (file.isDirectory()) {
                        updateDigestWithFolder(digest, file, rootPath, toCheck);
                    } else {
                        digest.update(relativePath.getBytes());
                        digest.update(computeFileHash(file));
                    }
                } else if (file.isDirectory() && isParentFolderInToCheck(relativePath, toCheck)) {
                    updateDigestWithFolder(digest, file, rootPath, toCheck);
                }
            }
        }
    }

    private boolean isParentFolderInToCheck(String relativePath, List<String> toCheck) {
        for (String path : toCheck) {
            if (relativePath.startsWith(path) && !relativePath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] computeFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest fileDigest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fileDigest.update(buffer, 0, bytesRead);
            }
        }
        return fileDigest.digest();
    }



}
