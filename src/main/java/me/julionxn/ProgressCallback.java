package me.julionxn;

@FunctionalInterface
public interface ProgressCallback {
    void onProgress(String status, float progress);
}
