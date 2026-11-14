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

package consulo.unreal.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A target as UnrealBuildTool exports it with {@code -Mode=JsonExport}: every module the target compiles or links,
 * engine modules included.
 */
public record UbtTarget(String name, String platform, String configuration, Map<String, UbtModule> modules) {
    /**
     * @param directory the directory holding the {@code .Build.cs} of the module
     */
    public record UbtModule(String name,
                            Path directory,
                            List<String> publicDependencies,
                            List<String> privateDependencies,
                            List<Path> publicIncludePaths,
                            List<Path> privateIncludePaths,
                            List<Path> publicSystemIncludePaths,
                            List<String> publicDefinitions) {
        public boolean isUnder(Path root) {
            return directory.startsWith(root);
        }
    }

    public @Nullable UbtModule findModule(String name) {
        return modules.get(name);
    }

    public static UbtTarget parse(String json) throws JsonParseException {
        JsonElement root = JsonParser.parseString(json);
        if (!(root instanceof JsonObject target)) {
            throw new JsonParseException("not a JSON object");
        }
        Map<String, UbtModule> modules = new LinkedHashMap<>();
        if (target.get("Modules") instanceof JsonObject moduleObjects) {
            for (Map.Entry<String, JsonElement> entry : moduleObjects.entrySet()) {
                if (!(entry.getValue() instanceof JsonObject module)) {
                    continue;
                }
                String directory = string(module, "Directory");
                if (directory == null) {
                    continue;
                }
                modules.put(entry.getKey(), new UbtModule(
                    entry.getKey(),
                    Path.of(directory).normalize(),
                    strings(module, "PublicDependencyModules"),
                    strings(module, "PrivateDependencyModules"),
                    paths(module, "PublicIncludePaths"),
                    paths(module, "PrivateIncludePaths"),
                    paths(module, "PublicSystemIncludePaths"),
                    strings(module, "PublicDefinitions")
                ));
            }
        }
        return new UbtTarget(
            String.valueOf(string(target, "Name")),
            String.valueOf(string(target, "Platform")),
            String.valueOf(string(target, "Configuration")),
            modules
        );
    }

    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
    }

    private static List<String> strings(JsonObject object, String name) {
        List<String> result = new ArrayList<>();
        if (object.get(name) instanceof JsonArray array) {
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    result.add(element.getAsString());
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<Path> paths(JsonObject object, String name) {
        return strings(object, name).stream().map(path -> Path.of(path).normalize()).toList();
    }
}
