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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Reads the {@code #define}s of the {@code Definitions.h} UnrealBuildTool generates for a module, following its
 * {@code #include} of the shared definitions header. Function-like macros are skipped. Other force-included headers
 * (precompiled header sources) are not read: they pull in the engine itself.
 */
final class DefinitionsHeader {
    private static final String DEFINE = "#define";
    private static final String UNDEF = "#undef";
    private static final String INCLUDE = "#include";
    private static final String DEFINITIONS_FILE_MARKER = "Definitions";

    private DefinitionsHeader() {
    }

    static void read(Path header, List<Path> includeDirectories, UnrealCompileSettings settings, Set<Path> visited) {
        Path fileName = header.getFileName();
        if (fileName == null || !fileName.toString().contains(DEFINITIONS_FILE_MARKER)) {
            return;
        }
        if (!visited.add(header) || !Files.isRegularFile(header)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(header);
        }
        catch (IOException e) {
            return;
        }
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith(DEFINE)) {
                define(line.substring(DEFINE.length()).trim(), settings);
            }
            else if (line.startsWith(UNDEF)) {
                settings.undefine(line.substring(UNDEF.length()).trim());
            }
            else if (line.startsWith(INCLUDE)) {
                String name = line.substring(INCLUDE.length()).trim();
                if (name.length() > 2 && name.startsWith("\"") && name.endsWith("\"")) {
                    Path included = find(header, name.substring(1, name.length() - 1), includeDirectories);
                    if (included != null) {
                        read(included, includeDirectories, settings, visited);
                    }
                }
            }
        }
    }

    private static void define(String definition, UnrealCompileSettings settings) {
        int end = 0;
        while (end < definition.length() && Character.isJavaIdentifierPart(definition.charAt(end))) {
            end++;
        }
        if (end == 0 || (end < definition.length() && definition.charAt(end) == '(')) {
            return;
        }
        settings.define(definition.substring(0, end), definition.substring(end).trim());
    }

    private static @Nullable Path find(Path header, String name, List<Path> includeDirectories) {
        Path parent = header.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve(name))) {
            return parent.resolve(name);
        }
        for (Path directory : includeDirectories) {
            Path candidate = directory.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
