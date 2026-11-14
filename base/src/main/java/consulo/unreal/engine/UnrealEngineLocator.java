/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.unreal.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import consulo.platform.Platform;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finds engine installations the way the engine itself does: the engines registered by {@code UnrealVersionSelector}
 * ({@code Install.ini}), the ones installed by the Epic launcher ({@code LauncherInstalled.dat}) and well-known locations.
 */
public final class UnrealEngineLocator {
    private static final String INSTALLATIONS_SECTION = "[Installations]";
    private static final String LAUNCHER_APP_PREFIX = "UE_";

    private UnrealEngineLocator() {
    }

    /**
     * @return engine association (a version like {@code 5.8} or the GUID of a source build) to engine home
     */
    public static Map<String, Path> findRegisteredEngines(Platform platform) {
        Map<String, Path> engines = new LinkedHashMap<>();
        readInstallIni(platform, engines);
        if (platform.os().isWindows()) {
            readLauncherInstalled(platform, engines);
        }
        return engines;
    }

    public static Set<Path> collectHomePaths(Platform platform) {
        Set<Path> homes = new LinkedHashSet<>(findRegisteredEngines(platform).values());
        Path userHome = platform.user().homePath();
        if (platform.os().isWindows()) {
            String programFiles = platform.os().getEnvironmentVariable("ProgramFiles");
            if (programFiles != null) {
                addChildren(homes, Path.of(programFiles, "Epic Games"));
            }
        }
        else if (platform.os().isMac()) {
            addChildren(homes, Path.of("/Users/Shared/Epic Games"));
        }
        else {
            homes.add(userHome.resolve("UnrealEngine"));
            homes.add(Path.of("/opt/UnrealEngine"));
            addChildren(homes, userHome.resolve("UnrealEngines"));
        }
        homes.removeIf(home -> !UnrealEngine.isValidHome(home));
        return homes;
    }

    /**
     * A project with an empty association lives inside a source build of the engine.
     */
    public static @Nullable Path findEngineAbove(Path projectDirectory) {
        for (Path directory = projectDirectory.getParent(); directory != null; directory = directory.getParent()) {
            if (UnrealEngine.isValidHome(directory)) {
                return directory;
            }
        }
        return null;
    }

    private static void readInstallIni(Platform platform, Map<String, Path> engines) {
        Path ini;
        if (platform.os().isWindows()) {
            // source builds on Windows are registered in HKCU\Software\Epic Games\Unreal Engine\Builds, not in a file
            return;
        }
        else if (platform.os().isMac()) {
            ini = platform.user().homePath().resolve("Library/Application Support/Epic/UnrealEngine/Install.ini");
        }
        else {
            ini = platform.user().homePath().resolve(".config/Epic/UnrealEngine/Install.ini");
        }
        if (!Files.isRegularFile(ini)) {
            return;
        }
        try {
            boolean inSection = false;
            for (String rawLine : Files.readAllLines(ini)) {
                String line = rawLine.trim();
                if (line.startsWith("[")) {
                    inSection = INSTALLATIONS_SECTION.equalsIgnoreCase(line);
                    continue;
                }
                int separator = line.indexOf('=');
                if (inSection && separator > 0) {
                    addEngine(engines, line.substring(0, separator).trim(), line.substring(separator + 1).trim());
                }
            }
        }
        catch (IOException ignored) {
        }
    }

    private static void readLauncherInstalled(Platform platform, Map<String, Path> engines) {
        String programData = platform.os().getEnvironmentVariable("ProgramData");
        if (programData == null) {
            return;
        }
        Path file = Path.of(programData, "Epic", "UnrealEngineLauncher", "LauncherInstalled.dat");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            JsonArray installations = root instanceof JsonObject object && object.get("InstallationList") instanceof JsonArray array
                ? array
                : new JsonArray();
            for (JsonElement element : installations) {
                if (!(element instanceof JsonObject installation)) {
                    continue;
                }
                String appName = string(installation, "AppName");
                String location = string(installation, "InstallLocation");
                if (appName != null && location != null && appName.startsWith(LAUNCHER_APP_PREFIX)) {
                    addEngine(engines, appName.substring(LAUNCHER_APP_PREFIX.length()), location);
                }
            }
        }
        catch (IOException | JsonParseException | IllegalStateException ignored) {
        }
    }

    private static void addEngine(Map<String, Path> engines, String association, String location) {
        if (association.isEmpty() || location.isEmpty()) {
            return;
        }
        try {
            engines.putIfAbsent(association, Path.of(location));
        }
        catch (InvalidPathException ignored) {
        }
    }

    private static void addChildren(Set<Path> homes, Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> children = Files.list(directory)) {
            homes.addAll(children.filter(Files::isDirectory).toList());
        }
        catch (IOException ignored) {
        }
    }

    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
    }
}
