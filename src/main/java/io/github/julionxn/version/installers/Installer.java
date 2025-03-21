package io.github.julionxn.version.installers;

import io.github.julionxn.system.Natives;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Installer {

    protected DownloadStatus downloadAndCheckFile(URL url, String hash, int expectedSize, File outputFile, Callable<DownloadStatus> onRedownload){
        if (outputFile.exists()){
            String fileHash = getSHA1Hash(outputFile);
            if (!hash.equals(fileHash)){
                boolean deleted = outputFile.delete();
                if (deleted) {
                    try {
                        return onRedownload.call();
                    } catch (Exception e) {
                        return DownloadStatus.ERROR;
                    }
                } else {
                    return DownloadStatus.DELETE_FILE_ERROR;
                }
            }
            return DownloadStatus.ALREADY_EXISTS;
        }
        return downloadFile(url, outputFile, hash, expectedSize);
    }

    protected DownloadStatus downloadFile(URL url, File outputFile, String hash, int expectedSize){
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return DownloadStatus.HTTP_ERROR;
            }
            int contentLength = connection.getContentLength();
            if (contentLength != expectedSize) {
                return DownloadStatus.SIZE_MISSMATCH;
            }
            writeToFileFromConnection(connection, outputFile);
            String outputFileHash = getSHA1Hash(outputFile);
            if (outputFileHash == null || !outputFileHash.equals(hash)){
                return DownloadStatus.HASH_MISSMATCH;
            } else {
                return DownloadStatus.OK;
            }
        } catch (IOException e) {
            return DownloadStatus.ERROR;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected DownloadStatus downloadFile(URL url, File outputFile){
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return DownloadStatus.HTTP_ERROR;
            }
            writeToFileFromConnection(connection, outputFile);
            return DownloadStatus.OK;
        } catch (IOException e) {
            return DownloadStatus.ERROR;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void writeToFileFromConnection(HttpURLConnection connection, File outputFile) throws IOException {
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    protected String getSHA1Hash(File file) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = messageDigest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    protected Natives getNatives(String nativeStr){
        if (nativeStr.contains("windows")){
            if (nativeStr.contains("64")){
                return Natives.WIN64;
            }
            if (nativeStr.contains("86")){
                return Natives.WIN86;
            }
            return Natives.WIN;
        }
        if (nativeStr.contains("macos") || nativeStr.contains("osx")){
            if (nativeStr.contains("64")){
                return Natives.OSX64;
            }
            return Natives.OSX;
        }
        if(nativeStr.contains("linux")){
            return Natives.LINUX;
        }
        return Natives.NONE;
    }

    protected <E, T extends Collection<E>> void executeConcurrent(T batches, Consumer<E> batchRunnable, int threadAmount){
        try (ExecutorService executor = Executors.newFixedThreadPool(threadAmount)){
            for (E batch : batches) {
                executor.submit(() -> batchRunnable.accept(batch));
            }
        }
    }

    protected <E> List<List<E>> splitIntoBatches(List<E> items, int batchAmount) {
        List<List<E>> batches = new ArrayList<>();
        int batchSize = (int) Math.ceil((double) items.size() / batchAmount);
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            batches.add(new ArrayList<>(items.subList(i, end)));
        }
        return batches;
    }

}
