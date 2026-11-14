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

import consulo.unreal.UnrealConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Compile settings of a module derived from the module rules alone, for when UnrealBuildTool cannot generate the compile
 * database (e.g. the project does not build yet). Follows the include path rules of {@code UEBuildModuleCPP}: the module's
 * own {@code Public}/{@code Private}/{@code Classes} directories, the public ones of its dependencies and of their public
 * dependencies, and the UnrealHeaderTool output directories.
 */
final class UbtModuleIncludes {
    private static final String[] PUBLIC_DIRECTORIES = {"Public", "Classes", "Internal"};

    private UbtModuleIncludes() {
    }

    static UnrealCompileSettings collect(UbtTarget target, UbtTarget.UbtModule module, Path projectDirectory, Path engineHome) {
        UnrealCompileSettings settings = new UnrealCompileSettings();
        String binaryType = target.name().endsWith(UnrealConstants.EDITOR_TARGET_SUFFIX) ? "UnrealEditor" : "UnrealGame";

        // the Source directory, so "<Module>/Header.h" resolves
        Path sourceDirectory = module.directory().getParent();
        if (sourceDirectory != null) {
            addIfExists(settings, sourceDirectory);
        }
        addIfExists(settings, module.directory());
        addIfExists(settings, module.directory().resolve("Private"));
        module.privateIncludePaths().forEach(path -> addIfExists(settings, path));

        Deque<UbtTarget.UbtModule> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(module);
        for (String dependency : module.privateDependencies()) {
            UbtTarget.UbtModule found = target.findModule(dependency);
            if (found != null) {
                queue.add(found);
            }
        }
        while (!queue.isEmpty()) {
            UbtTarget.UbtModule current = queue.poll();
            if (!visited.add(current.name())) {
                continue;
            }
            for (String directory : PUBLIC_DIRECTORIES) {
                addIfExists(settings, current.directory().resolve(directory));
            }
            current.publicIncludePaths().forEach(path -> addIfExists(settings, path));
            current.publicSystemIncludePaths().forEach(path -> addIfExists(settings, path));
            Path intermediateRoot = current.isUnder(projectDirectory) ? projectDirectory : engineHome.resolve("Engine");
            addIfExists(settings, intermediateRoot.resolve(UnrealConstants.INTERMEDIATE_DIRECTORY).resolve("Build")
                .resolve(target.platform()).resolve(binaryType).resolve("Inc").resolve(current.name()).resolve("UHT"));

            settings.define(current.name().toUpperCase(Locale.ROOT) + "_API", "");
            for (String definition : current.publicDefinitions()) {
                int equals = definition.indexOf('=');
                settings.define(equals < 0 ? definition : definition.substring(0, equals), equals < 0 ? "1" : definition.substring(equals + 1));
            }
            for (String dependency : current.publicDependencies()) {
                UbtTarget.UbtModule found = target.findModule(dependency);
                if (found != null) {
                    queue.add(found);
                }
            }
        }
        if (binaryType.equals("UnrealEditor")) {
            settings.define("WITH_EDITOR", "1");
            settings.define("WITH_EDITORONLY_DATA", "1");
        }
        settings.define("UE_BUILD_" + target.configuration().toUpperCase(Locale.ROOT), "1");
        return settings;
    }

    private static void addIfExists(UnrealCompileSettings settings, Path directory) {
        if (Files.isDirectory(directory)) {
            settings.addIncludePath(directory.toString());
        }
    }
}
