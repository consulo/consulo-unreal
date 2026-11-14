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
import consulo.process.cmd.ParametersListUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the {@code compile_commands.json} of {@code -Mode=GenerateClangDatabase}. UnrealBuildTool keeps the real compiler
 * arguments in response files ({@code @Module.Shared.rsp}) and the defines in a force-included {@code Definitions.h}, so
 * both are followed.
 */
public final class UbtCompileDatabase {
    /**
     * Options whose value we read (or must skip), longest first where one is a prefix of another.
     */
    private static final List<String> FLAGS = List.of("-include-pch", "-isystem", "-iquote", "-include", "-I", "-D", "-U");

    public record Entry(Path file, Path directory, String command) {
    }

    private UbtCompileDatabase() {
    }

    public static List<Entry> read(Path compileCommands) throws IOException, JsonParseException {
        JsonElement root = JsonParser.parseString(Files.readString(compileCommands));
        if (!(root instanceof JsonArray entries)) {
            throw new JsonParseException("not a JSON array");
        }
        List<Entry> result = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!(element instanceof JsonObject entry)) {
                continue;
            }
            String file = string(entry, "file");
            String directory = string(entry, "directory");
            String command = string(entry, "command");
            if (file != null && directory != null && command != null) {
                result.add(new Entry(Path.of(file).normalize(), Path.of(directory), command));
            }
        }
        return result;
    }

    public static UnrealCompileSettings toCompileSettings(Entry entry) {
        List<String> arguments = new ArrayList<>();
        List<String> command = ParametersListUtil.parse(entry.command());
        expand(command.subList(Math.min(1, command.size()), command.size()), entry.directory(), arguments, new HashSet<>());

        UnrealCompileSettings settings = new UnrealCompileSettings();
        List<String> systemIncludePaths = new ArrayList<>();
        List<Path> forcedIncludes = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (argument.startsWith("-std=")) {
                settings.setCppStandard(argument.substring("-std=".length()));
                continue;
            }
            String flag = findFlag(argument);
            if (flag == null) {
                continue;
            }
            String value;
            if (argument.length() > flag.length()) {
                value = argument.substring(flag.length());
            }
            else if (i + 1 < arguments.size()) {
                value = arguments.get(++i);
            }
            else {
                break;
            }
            switch (flag) {
                case "-isystem" -> systemIncludePaths.add(resolve(entry.directory(), value));
                case "-iquote", "-I" -> settings.addIncludePath(resolve(entry.directory(), value));
                case "-include" -> forcedIncludes.add(Path.of(resolve(entry.directory(), value)));
                case "-D" -> {
                    int equals = value.indexOf('=');
                    settings.define(equals < 0 ? value : value.substring(0, equals), equals < 0 ? "1" : value.substring(equals + 1));
                }
                case "-U" -> settings.undefine(value);
                default -> {
                    // an option with a separate value we do not need, e.g. -include-pch
                }
            }
        }
        systemIncludePaths.forEach(settings::addIncludePath);

        List<Path> includeDirectories = settings.getIncludePaths().stream().map(Path::of).toList();
        Set<Path> visited = new HashSet<>();
        for (Path forcedInclude : forcedIncludes) {
            DefinitionsHeader.read(forcedInclude, includeDirectories, settings, visited);
        }
        return settings;
    }

    /**
     * @return the option an argument starts with, {@code null} for an option we do not read
     */
    private static @Nullable String findFlag(String argument) {
        for (String flag : FLAGS) {
            if (argument.startsWith(flag)) {
                return flag;
            }
        }
        return null;
    }

    private static void expand(List<String> arguments, Path directory, List<String> result, Set<Path> visited) {
        for (String argument : arguments) {
            if (!argument.startsWith("@")) {
                result.add(argument);
                continue;
            }
            Path responseFile = Path.of(resolve(directory, argument.substring(1)));
            if (!visited.add(responseFile)) {
                continue;
            }
            try {
                expand(ParametersListUtil.parse(Files.readString(responseFile)), directory, result, visited);
            }
            catch (IOException | InvalidPathException ignored) {
            }
        }
    }

    private static String resolve(Path directory, String path) {
        String unquoted = path.length() > 1 && path.startsWith("\"") && path.endsWith("\"") ? path.substring(1, path.length() - 1) : path;
        return directory.resolve(unquoted).normalize().toString();
    }

    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
    }
}
