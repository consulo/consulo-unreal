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

import com.google.gson.JsonParseException;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ContentRootData;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.project.ModuleDependencyData;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.rt.model.ExternalSystemSourceType;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.service.project.ProjectData;
import consulo.localize.LocalizeValue;
import consulo.unreal.UnrealConstants;
import consulo.unreal.UnrealUtil;
import consulo.unreal.engine.UnrealEngine;
import consulo.unreal.localize.UnrealLocalize;
import consulo.unreal.run.UnrealRunData;
import consulo.unreal.setting.UnrealExecutionSettings;
import consulo.unreal.task.UnrealCommand;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static consulo.unreal.UnrealConstants.SYSTEM_ID;

/**
 * Turns a project into modules with UnrealBuildTool:
 * <ul>
 * <li>{@code <Project>.uproject} - the project directory, its build output excluded</li>
 * <li>one module per C++ module of the project and its plugins ({@code -Mode=JsonExport}), with the include paths and
 * defines its sources compile with ({@code -Mode=GenerateClangDatabase})</li>
 * <li>{@code UE <version>} - a module without content exporting the library {@code UE <version> Sources} of
 * {@code Engine/Source}, so engine headers are indexed and resolve their own includes; UnrealBuildTool's sources are the
 * library {@code UE <version> Build Sources} of the build scripts module</li>
 * </ul>
 */
public class UnrealProjectResolver implements ExternalSystemProjectResolver<UnrealExecutionSettings> {
    /**
     * Lets a target change the build settings while sharing the engine's build environment - Lyra's editor target does,
     * which an engine newer than the project refuses ("This is not allowed, as LyraEditor has build products in common
     * with UnrealEditor"). Exporting builds nothing, so the engine's products are safe.
     */
    private static final String OVERRIDE_BUILD_ENVIRONMENT = "-OverrideBuildEnvironment";

    private static final String OUTPUT_DIRECTORY = "Consulo";
    private static final String COMPILE_COMMANDS = "compile_commands.json";

    private final UnrealCommand myCommand = new UnrealCommand();

    @Override
    public @Nullable DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
                                                              String projectPath,
                                                              boolean isPreviewMode,
                                                              @Nullable UnrealExecutionSettings settings,
                                                              ExternalSystemTaskNotificationListener listener)
        throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
        UProjectFile projectFile = UnrealUtil.readProjectFile(projectPath);
        Path projectDirectory = projectFile.getDirectory();
        status(id, listener, UnrealLocalize.statusReadingProject(projectFile.file().getFileName()));

        ProjectData projectData = new ProjectData(SYSTEM_ID, projectFile.name(), projectDirectory.toString(), projectPath);
        DataNode<ProjectData> projectNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);
        addTasks(projectNode, projectPath);

        // Consulo modules are directory based: every module needs its own directory and a name of its own, and the
        // game module usually has the name of the project
        String projectModuleName = projectFile.name() + UnrealConstants.PROJECT_FILE_EXTENSION;
        createProjectModule(projectNode, projectModuleName, projectDirectory, projectPath);
        projectNode.createChild(UnrealRunData.KEY, new UnrealRunData(projectModuleName, projectFile.name()));

        String target = UnrealUtil.findTarget(projectFile, settings == null ? null : settings.getTarget());
        if (isPreviewMode || target == null) {
            // Blueprint-only projects have no C++ to import
            return projectNode;
        }

        UnrealEngine engine = UnrealUtil.toEngine(settings, projectFile);
        projectNode.createChild(UnrealRulesData.KEY, createRulesData(projectFile, engine));
        String configuration = settings == null ? UnrealConstants.DEFAULT_CONFIGURATION : settings.getConfiguration();
        Path outputDirectory = projectDirectory.resolve(UnrealConstants.INTERMEDIATE_DIRECTORY).resolve(OUTPUT_DIRECTORY);
        try {
            Files.createDirectories(outputDirectory);
        }
        catch (IOException e) {
            throw new ExternalSystemException(e);
        }

        UbtTarget ubtTarget = exportTarget(id, engine, projectFile, target, configuration, outputDirectory, listener);
        List<UbtTarget.UbtModule> projectModules = ubtTarget.modules().values().stream()
            .filter(module -> module.isUnder(projectDirectory))
            .toList();
        Map<String, UnrealCompileSettings> compileSettings =
            collectCompileSettings(id, engine, projectFile, ubtTarget, projectModules, configuration, outputDirectory, listener);

        DataNode<ModuleData> engineModuleNode = null;
        if (settings == null || settings.isIndexEngineSources()) {
            engineModuleNode = createEngineModule(projectNode, engine, projectPath, projectFile.name() + UnrealConstants.RULES_MODULE_SUFFIX,
                compileSettings.values());
        }

        Map<String, DataNode<ModuleData>> moduleNodes = new LinkedHashMap<>();
        for (UbtTarget.UbtModule module : projectModules) {
            ModuleData moduleData = new ModuleData(module.name(), SYSTEM_ID, module.name(), module.directory().toString(), projectPath);
            DataNode<ModuleData> moduleNode = projectNode.createChild(ProjectKeys.MODULE, moduleData);
            ContentRootData contentRoot = new ContentRootData(SYSTEM_ID, module.directory().toString());
            contentRoot.storePath(ExternalSystemSourceType.SOURCE, module.directory().toString());
            moduleNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
            moduleNode.createChild(UnrealCompileData.KEY, toCompileData(compileSettings.get(module.name())));
            if (engineModuleNode != null) {
                moduleNode.createChild(ProjectKeys.MODULE_DEPENDENCY, new ModuleDependencyData(moduleData, engineModuleNode.getData()));
            }
            moduleNodes.put(module.name(), moduleNode);
        }
        for (UbtTarget.UbtModule module : projectModules) {
            DataNode<ModuleData> moduleNode = moduleNodes.get(module.name());
            List<String> dependencies = new ArrayList<>(module.publicDependencies());
            dependencies.addAll(module.privateDependencies());
            for (String dependency : dependencies.stream().distinct().toList()) {
                DataNode<ModuleData> dependencyNode = moduleNodes.get(dependency);
                if (moduleNode != null && dependencyNode != null && dependencyNode != moduleNode) {
                    moduleNode.createChild(ProjectKeys.MODULE_DEPENDENCY, new ModuleDependencyData(moduleNode.getData(), dependencyNode.getData()));
                }
            }
        }
        return projectNode;
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId taskId, ExternalSystemTaskNotificationListener listener) {
        return myCommand.cancel();
    }

    private UbtTarget exportTarget(ExternalSystemTaskId id,
                                   UnrealEngine engine,
                                   UProjectFile projectFile,
                                   String target,
                                   String configuration,
                                   Path outputDirectory,
                                   ExternalSystemTaskNotificationListener listener) {
        status(id, listener, UnrealLocalize.statusExportingModules(target));
        Path exportFile = outputDirectory.resolve(target + ".json");
        List<String> arguments = List.of("-Mode=JsonExport", OVERRIDE_BUILD_ENVIRONMENT, "-OutputFile=" + exportFile);
        myCommand.run(id, engine.createBuildCommandLine(target, configuration, projectFile.file(), arguments), "UnrealBuildTool -Mode=JsonExport", listener);
        try {
            return UbtTarget.parse(Files.readString(exportFile));
        }
        catch (IOException | JsonParseException | IllegalStateException e) {
            throw new ExternalSystemException(UnrealLocalize.errorBadOutput(exportFile, String.valueOf(e.getMessage())).get());
        }
    }

    /**
     * Settings of every project module: what its sources compile with, or what the module rules imply when the compile
     * database cannot be generated.
     */
    private Map<String, UnrealCompileSettings> collectCompileSettings(ExternalSystemTaskId id,
                                                                     UnrealEngine engine,
                                                                     UProjectFile projectFile,
                                                                     UbtTarget ubtTarget,
                                                                     List<UbtTarget.UbtModule> projectModules,
                                                                     String configuration,
                                                                     Path outputDirectory,
                                                                     ExternalSystemTaskNotificationListener listener) {
        status(id, listener, UnrealLocalize.statusGeneratingCompileDatabase(ubtTarget.name()));
        List<UbtCompileDatabase.Entry> entries = List.of();
        try {
            List<String> arguments = List.of("-Mode=GenerateClangDatabase", OVERRIDE_BUILD_ENVIRONMENT, "-OutputDir=" + outputDirectory);
            myCommand.run(id, engine.createBuildCommandLine(ubtTarget.name(), configuration, projectFile.file(), arguments),
                "UnrealBuildTool -Mode=GenerateClangDatabase", listener);
            entries = UbtCompileDatabase.read(outputDirectory.resolve(COMPILE_COMMANDS));
        }
        catch (ExternalSystemException | IOException | JsonParseException | IllegalStateException e) {
            listener.onTaskOutput(id, UnrealLocalize.warningCompileDatabaseFailed(String.valueOf(e.getMessage())).get() + "\n", false);
        }

        Map<String, UnrealCompileSettings> result = new LinkedHashMap<>();
        for (UbtTarget.UbtModule module : projectModules) {
            UbtCompileDatabase.Entry entry = entries.stream().filter(candidate -> candidate.file().startsWith(module.directory())).findFirst().orElse(null);
            result.put(module.name(), entry != null
                ? UbtCompileDatabase.toCompileSettings(entry)
                : UbtModuleIncludes.collect(ubtTarget, module, projectFile.getDirectory(), engine.getHome()));
        }
        return result;
    }

    /**
     * The module of the project directory, its build output, packaging files and the build output of its plugins
     * excluded.
     */
    static DataNode<ModuleData> createProjectModule(DataNode<ProjectData> projectNode,
                                                    String projectModuleName,
                                                    Path projectDirectory,
                                                    String projectPath) {
        ModuleData projectModule = new ModuleData(projectModuleName, SYSTEM_ID, projectModuleName, projectDirectory.toString(), projectPath);
        DataNode<ModuleData> projectModuleNode = projectNode.createChild(ProjectKeys.MODULE, projectModule);
        ContentRootData projectRoot = new ContentRootData(SYSTEM_ID, projectDirectory.toString());
        for (String excluded : UnrealConstants.EXCLUDED_DIRECTORIES) {
            projectRoot.storePath(ExternalSystemSourceType.EXCLUDED, projectDirectory.resolve(excluded).toString());
        }
        for (String excluded : UnrealConstants.EXCLUDED_PROJECT_DIRECTORIES) {
            projectRoot.storePath(ExternalSystemSourceType.EXCLUDED, projectDirectory.resolve(excluded).toString());
        }
        for (Path pluginDirectory : UnrealUtil.findPluginDirectories(projectDirectory)) {
            for (String excluded : UnrealConstants.EXCLUDED_PLUGIN_DIRECTORIES) {
                Path directory = pluginDirectory.resolve(excluded);
                if (Files.isDirectory(directory)) {
                    projectRoot.storePath(ExternalSystemSourceType.EXCLUDED, directory.toString());
                }
            }
        }
        projectModuleNode.createChild(ProjectKeys.CONTENT_ROOT, projectRoot);
        return projectModuleNode;
    }

    /**
     * The engine as the project sees it, without content of its own - its sources are never edited in a game project:
     * <ul>
     * <li>a module exporting the library {@code UE <version> Sources} of {@code Engine/Source}. Every project module
     * depends on it, so the engine headers are indexed once, with the defines and include paths of all the project
     * modules together</li>
     * <li>the library {@code UE <version> Build Sources} of UnrealBuildTool and the {@code EpicGames.*} libraries, for the
     * module of the build scripts, which compile against them</li>
     * </ul>
     */
    static DataNode<ModuleData> createEngineModule(DataNode<ProjectData> projectNode,
                                                   UnrealEngine engine,
                                                   String projectPath,
                                                   String rulesModuleName,
                                                   Iterable<UnrealCompileSettings> projectSettings) {
        String name = engineModuleName(engine);
        Path sourceDirectory = engine.getSourceDirectory();
        ModuleData moduleData = new ModuleData(name, SYSTEM_ID, name, sourceDirectory.toString(), projectPath);
        DataNode<ModuleData> moduleNode = projectNode.createChild(ProjectKeys.MODULE, moduleData);

        UnrealCompileSettings settings = new UnrealCompileSettings();
        projectSettings.forEach(settings::addAll);
        moduleNode.createChild(UnrealCompileData.KEY, toCompileData(settings));

        projectNode.createChild(UnrealEngineLibraryData.KEY, new UnrealEngineLibraryData(
            name + UnrealConstants.ENGINE_LIBRARY_SUFFIX,
            List.of(sourceDirectory.toString()),
            UnrealUtil.findEngineLibraryExcludes(sourceDirectory),
            List.of(name)
        ));

        List<String> buildSources = new ArrayList<>();
        for (String program : UnrealConstants.KEPT_ENGINE_PROGRAMS) {
            Path directory = sourceDirectory.resolve(UnrealConstants.ENGINE_PROGRAMS_DIRECTORY).resolve(program);
            if (Files.isDirectory(directory)) {
                buildSources.add(directory.toString());
            }
        }
        if (!buildSources.isEmpty()) {
            projectNode.createChild(UnrealEngineLibraryData.KEY, new UnrealEngineLibraryData(
                name + UnrealConstants.ENGINE_BUILD_LIBRARY_SUFFIX,
                buildSources,
                List.of(),
                List.of(rulesModuleName)
            ));
        }
        return moduleNode;
    }

    private static UnrealRulesData createRulesData(UProjectFile projectFile, UnrealEngine engine) {
        List<String> files = UnrealUtil.findBuildFiles(projectFile.getDirectory()).stream()
            .filter(UnrealUtil::isRulesFile)
            .map(Path::toString)
            .toList();
        return new UnrealRulesData(
            projectFile.name() + UnrealConstants.RULES_MODULE_SUFFIX,
            files,
            UnrealConstants.RULES_EXTENSION_IDS,
            engine.getHome().toString(),
            // the UnrealBuildTool sources come as the library UE <version> Build Sources
            List.of()
        );
    }

    private static String engineModuleName(UnrealEngine engine) {
        String version = engine.readVersion();
        return version == null ? "UE" : "UE " + version;
    }

    private static UnrealCompileData toCompileData(@Nullable UnrealCompileSettings settings) {
        if (settings == null) {
            return new UnrealCompileData(List.of(), Map.of(), null);
        }
        // UnrealBuildTool also lists directories that only exist for some platforms or after a build
        List<String> includePaths = settings.getIncludePaths().stream().filter(path -> Files.isDirectory(Path.of(path))).toList();
        return new UnrealCompileData(includePaths, settings.getDefines(), toLanguageVersionId(settings.getCppStandard()));
    }

    /**
     * {@code c++20}, {@code gnu++2a} → {@code CPP20}
     */
    static @Nullable String toLanguageVersionId(@Nullable String standard) {
        if (standard == null) {
            return null;
        }
        String value = standard.toLowerCase(Locale.ROOT);
        int plus = value.indexOf("++");
        if (plus < 0) {
            return null;
        }
        String version = switch (value.substring(plus + 2)) {
            case "98", "03" -> "98";
            case "0x", "11" -> "11";
            case "1y", "14" -> "14";
            case "1z", "17" -> "17";
            case "2a", "20" -> "20";
            case "2b", "23", "2c", "26" -> "23";
            default -> null;
        };
        return version == null ? null : "CPP" + version;
    }

    private static void addTasks(DataNode<ProjectData> projectNode, String projectPath) {
        Map<String, LocalizeValue> tasks = new LinkedHashMap<>();
        tasks.put(UnrealConstants.TASK_BUILD, UnrealLocalize.taskBuildDescription());
        tasks.put(UnrealConstants.TASK_REBUILD, UnrealLocalize.taskRebuildDescription());
        tasks.put(UnrealConstants.TASK_CLEAN, UnrealLocalize.taskCleanDescription());
        tasks.put(UnrealConstants.TASK_EDITOR, UnrealLocalize.taskEditorDescription());
        tasks.put(UnrealConstants.TASK_GAME, UnrealLocalize.taskGameDescription());
        for (Map.Entry<String, LocalizeValue> task : tasks.entrySet()) {
            projectNode.createChild(ProjectKeys.TASK, new TaskData(SYSTEM_ID, task.getKey(), projectPath, task.getValue().get()));
        }
    }

    private static void status(ExternalSystemTaskId id, ExternalSystemTaskNotificationListener listener, LocalizeValue message) {
        listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, message.get()));
    }
}
