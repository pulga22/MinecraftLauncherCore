package me.julionxn.version.data;

import com.google.gson.JsonObject;

public record RuntimeComponentInfo(String version, JsonObject runtimeData) {
}
