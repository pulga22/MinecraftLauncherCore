package me.julionxn.versions;

import com.google.gson.JsonObject;

public record RuntimeComponentInfo(String version, JsonObject runtimeData) {
}
