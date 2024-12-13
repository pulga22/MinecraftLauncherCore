package io.github.julionxn.system;

public enum Natives {
    LINUX, WIN86, WIN64, WIN, OSX, OSX64, NONE, WIN_ARM64, OSX_ARM64, LINUX_ARM, LINUX_OTHER;

    public static String getNativesString(Natives natives) {
        return switch (natives) {
            case LINUX -> "linux";
            case WIN86 -> "windows-x86";  // 32-bit Windows
            case WIN64 -> "windows-x64";  // 64-bit Windows
            case WIN -> "windows";  // General Windows, fallback
            case OSX -> "mac-os";  // macOS (Intel-based)
            case OSX64 -> "mac-os";
            case WIN_ARM64 -> "windows-arm64";  // ARM 64-bit Windows
            case OSX_ARM64 -> "mac-os-arm64";  // ARM 64-bit macOS
            case LINUX_ARM -> "linux";  // General Linux ARM
            case LINUX_OTHER -> "linux";  // For other Linux architectures
            default -> "none";
        };
    }

}
