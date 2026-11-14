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

package consulo.unreal;

import com.google.gson.JsonParseException;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.unreal.engine.UnrealEngine;
import consulo.unreal.engine.UnrealEngineLocator;
import consulo.unreal.engine.UnrealEngineSdkType;
import consulo.unreal.localize.UnrealLocalize;
import consulo.unreal.project.UProjectFile;
import consulo.unreal.setting.UnrealExecutionSettings;
import consulo.unreal.setting.UnrealProjectSettings;
import consulo.unreal.setting.UnrealSettings;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class UnrealUtil {
    private static final int MAX_BUILD_FILE_DEPTH = 8;

    private UnrealUtil() {
    }

    public static boolean isProjectFile(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(UnrealConstants.PROJECT_FILE_EXTENSION) && !Files.isDirectory(path);
    }

    /**
     * @param fileOrDirectory a {@code .uproject} file or the directory holding one
     */
    public static @Nullable Path findProjectFile(Path fileOrDirectory) {
        if (isProjectFile(fileOrDirectory)) {
            return fileOrDirectory;
        }
        if (!Files.isDirectory(fileOrDirectory)) {
            return null;
        }
        try (Stream<Path> children = Files.list(fileOrDirectory)) {
            return children.filter(UnrealUtil::isProjectFile).sorted().findFirst().orElse(null);
        }
        catch (IOException e) {
            return null;
        }
    }

    public static Path toProjectDirectory(Path fileOrDirectory) {
        if (Files.isDirectory(fileOrDirectory)) {
            return fileOrDirectory;
        }
        Path parent = fileOrDirectory.getParent();
        return parent == null ? fileOrDirectory : parent;
    }

    /**
     * A project may be linked by its directory or by its {@code .uproject} - both name the same settings.
     */
    public static @Nullable UnrealProjectSettings findProjectSettings(Project project, String projectPath) {
        Path projectDirectory = toProjectDirectory(Path.of(projectPath));
        for (UnrealProjectSettings settings : UnrealSettings.getInstance(project).getLinkedProjectsSettings()) {
            String linkedProjectPath = settings.getExternalProjectPath();
            if (linkedProjectPath != null && toProjectDirectory(Path.of(linkedProjectPath)).equals(projectDirectory)) {
                return settings;
            }
        }
        return null;
    }

    public static UnrealExecutionSettings createExecutionSettings(Project project, String linkedProjectPath) {
        UnrealProjectSettings settings = findProjectSettings(project, linkedProjectPath);
        Path engineHome = findEngineHome(settings, linkedProjectPath);
        return new UnrealExecutionSettings(
            engineHome == null ? null : engineHome.toString(),
            settings == null ? null : settings.getTarget(),
            settings == null ? UnrealConstants.DEFAULT_CONFIGURATION : settings.getConfiguration(),
            settings == null || settings.isIndexEngineSources()
        );
    }

    /**
     * The engine chosen for the project, else the one its {@code EngineAssociation} names: the engine tree around the
     * project, a bundle of that version, a registered engine, and at last the most recent bundle.
     */
    public static @Nullable Path findEngineHome(@Nullable UnrealProjectSettings settings, String linkedProjectPath) {
        SdkTable sdkTable = SdkTable.getInstance();
        UnrealEngineSdkType sdkType = UnrealEngineSdkType.getInstance();
        String engineName = settings == null ? null : settings.getEngineName();
        if (engineName != null) {
            UnrealEngine engine = UnrealEngine.fromSdk(sdkTable.findSdk(engineName));
            if (engine != null) {
                return engine.getHome();
            }
        }

        Path projectFile = findProjectFile(Path.of(linkedProjectPath));
        String association = projectFile == null ? "" : readEngineAssociation(projectFile);
        if (projectFile != null && association.isEmpty()) {
            Path engineAbove = UnrealEngineLocator.findEngineAbove(toProjectDirectory(projectFile));
            if (engineAbove != null) {
                return engineAbove;
            }
        }
        if (!association.isEmpty()) {
            for (Sdk sdk : sdkTable.getSdksOfType(sdkType)) {
                String version = sdk.getVersionString();
                UnrealEngine engine = UnrealEngine.fromSdk(sdk);
                if (engine != null && version != null && (version.equals(association) || version.startsWith(association + "."))) {
                    return engine.getHome();
                }
            }
            Path registered = UnrealEngineLocator.findRegisteredEngines(Platform.current()).get(association);
            if (registered != null && UnrealEngine.isValidHome(registered)) {
                return registered;
            }
        }
        UnrealEngine mostRecent = UnrealEngine.fromSdk(sdkTable.findMostRecentSdkOfType(sdkType));
        return mostRecent == null ? null : mostRecent.getHome();
    }

    /**
     * The engine resolved on the IDE side for a resolve/task run.
     */
    public static UnrealEngine toEngine(@Nullable UnrealExecutionSettings settings, UProjectFile projectFile) throws ExternalSystemException {
        String home = settings == null ? null : settings.getEngineHome();
        if (home == null || !UnrealEngine.isValidHome(Path.of(home))) {
            throw new ExternalSystemException(UnrealLocalize.errorNoEngine(projectFile.engineAssociation()).get());
        }
        return new UnrealEngine(Platform.current(), Path.of(home));
    }

    public static UProjectFile readProjectFile(String projectPath) throws ExternalSystemException {
        Path projectFile = findProjectFile(Path.of(projectPath));
        if (projectFile == null) {
            throw new IllegalArgumentException(UnrealLocalize.errorNoProjectFile(projectPath).get());
        }
        try {
            return UProjectFile.read(projectFile);
        }
        catch (IOException | JsonParseException e) {
            throw new ExternalSystemException(UnrealLocalize.errorBadProjectFile(projectFile, String.valueOf(e.getMessage())).get());
        }
    }

    /**
     * The target to import and build: the one chosen for the project, else its editor target, else the first one.
     */
    public static @Nullable String findTarget(UProjectFile projectFile, @Nullable String chosenTarget) {
        if (chosenTarget != null) {
            return chosenTarget;
        }
        List<String> targets = findTargets(projectFile.getDirectory());
        String editorTarget = projectFile.name() + UnrealConstants.EDITOR_TARGET_SUFFIX;
        if (targets.contains(editorTarget)) {
            return editorTarget;
        }
        return targets.stream().filter(target -> target.endsWith(UnrealConstants.EDITOR_TARGET_SUFFIX)).findFirst()
            .orElse(targets.isEmpty() ? null : targets.get(0));
    }

    /**
     * Names of the {@code Source/*.Target.cs} files of a project.
     */
    public static List<String> findTargets(Path projectDirectory) {
        Path source = projectDirectory.resolve(UnrealConstants.SOURCE_DIRECTORY);
        if (!Files.isDirectory(source)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(source)) {
            return children.map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(UnrealConstants.TARGET_RULES_SUFFIX))
                .map(name -> name.substring(0, name.length() - UnrealConstants.TARGET_RULES_SUFFIX.length()))
                .sorted()
                .toList();
        }
        catch (IOException e) {
            return List.of();
        }
    }

    /**
     * {@code .uproject}, {@code .uplugin}, {@code .Build.cs} and {@code .Target.cs} files - what UnrealBuildTool reads.
     */
    public static boolean isBuildFile(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString();
        return name.endsWith(UnrealConstants.PROJECT_FILE_EXTENSION)
            || name.endsWith(UnrealConstants.PLUGIN_FILE_EXTENSION)
            || isRulesFile(path);
    }

    /**
     * {@code .Build.cs} and {@code .Target.cs} - the C# build scripts.
     */
    public static boolean isRulesFile(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString();
        return name.endsWith(UnrealConstants.BUILD_RULES_SUFFIX) || name.endsWith(UnrealConstants.TARGET_RULES_SUFFIX);
    }

    /**
     * Build files of the project's {@code Source} and {@code Plugins} directories, build output skipped.
     */
    public static List<Path> findBuildFiles(Path projectDirectory) {
        List<Path> files = new ArrayList<>();
        for (String directory : List.of(UnrealConstants.SOURCE_DIRECTORY, UnrealConstants.PLUGINS_DIRECTORY)) {
            Path root = projectDirectory.resolve(directory);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root, MAX_BUILD_FILE_DEPTH)) {
                paths.filter(path -> isBuildFile(path) && !isUnderBuildOutput(root, path)).sorted().forEach(files::add);
            }
            catch (IOException ignored) {
            }
        }
        return files;
    }

    private static boolean isUnderBuildOutput(Path root, Path path) {
        for (Path part : root.relativize(path)) {
            if (UnrealConstants.EXCLUDED_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static String readEngineAssociation(Path projectFile) {
        try {
            return UProjectFile.read(projectFile).engineAssociation();
        }
        catch (IOException | JsonParseException e) {
            return "";
        }
    }
}
