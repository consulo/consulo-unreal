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
import consulo.unreal.UnrealConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The parts of a {@code .uproject} the importer needs.
 *
 * @param engineAssociation version of an installed engine ({@code 5.8}), GUID of a registered source build, or empty when
 *                          the project lives inside the engine tree
 * @param moduleNames       the C++ modules of the project; empty for a Blueprint-only project
 */
public record UProjectFile(Path file, String name, String engineAssociation, List<String> moduleNames) {
    public Path getDirectory() {
        Path parent = file.getParent();
        return parent == null ? file : parent;
    }

    public static UProjectFile read(Path file) throws IOException, JsonParseException {
        String fileName = file.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - UnrealConstants.PROJECT_FILE_EXTENSION.length());
        JsonElement root = JsonParser.parseString(Files.readString(file));
        if (!(root instanceof JsonObject project)) {
            throw new JsonParseException("not a JSON object");
        }
        JsonElement association = project.get("EngineAssociation");
        String engineAssociation = association == null || !association.isJsonPrimitive() ? "" : association.getAsString();
        List<String> moduleNames = new ArrayList<>();
        if (project.get("Modules") instanceof JsonArray modules) {
            for (JsonElement element : modules) {
                if (element instanceof JsonObject module && module.get("Name") instanceof JsonElement moduleName && moduleName.isJsonPrimitive()) {
                    moduleNames.add(moduleName.getAsString());
                }
            }
        }
        return new UProjectFile(file, name, engineAssociation.trim(), List.copyOf(moduleNames));
    }
}
