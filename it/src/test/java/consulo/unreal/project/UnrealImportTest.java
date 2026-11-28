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

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.content.library.Library;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ContentRootData;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.project.ModuleDependencyData;
import consulo.externalSystem.rt.model.ExternalSystemSourceType;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.index.impl.internal.UnindexedFilesScanner;
import consulo.language.index.impl.internal.UnindexedFilesScannerExecutorImpl;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.unreal.UnrealConstants;
import consulo.unreal.engine.UnrealEngine;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The project structure an Unreal import creates, applied the way an import applies it - without UnrealBuildTool: the
 * project module with its excludes, the engine module of {@code Engine/Source/Programs} exporting the library of the
 * engine sources, and a game module depending on it.
 *
 * @author VISTALL
 * @since 2026-11-28
 */
@ExtendWith(HeadlessProjectExtension.class)
public class UnrealImportTest {
    private static final String PROJECT = "Game";
    private static final String ENGINE_MODULE = "UE 5.8.0";

    @Test
    public void importCreatesTheEngineLibraryWithItsExcludes(HeadlessProjects projects) throws Exception {
        Path projectDirectory = createProject(projects.newDirectory());
        Path engineHome = createEngine(projects.newDirectory());
        Project project = projects.open(projectDirectory);
        HeadlessProjects.awaitIdle(project);

        importData(project, projectDirectory, engineHome);
        HeadlessProjects.awaitIdle(project);

        Path source = engineHome.resolve("Engine/Source");
        Library library = ReadAction.compute(() ->
            ProjectLibraryTable.getInstance(project).getLibraryByName(ENGINE_MODULE + UnrealConstants.ENGINE_LIBRARY_SUFFIX));
        assertNotNull(library, "the engine library must be created");
        assertEquals(List.of(VirtualFileUtil.pathToUrl(source.toString())), List.of(library.getUrls(BinariesOrderRootType.ID)));
        assertEquals(urls(source.resolve("Programs"), source.resolve("Runtime/Core/Private"), source.resolve("ThirdParty")),
            new TreeSet<>(List.of(library.getExcludedRootUrls())));

        Module engineModule = findModule(project, ENGINE_MODULE);
        LibraryOrderEntry libraryEntry = ReadAction.compute(() -> findLibraryEntry(engineModule, library));
        assertNotNull(libraryEntry, "the engine module must have the engine library");
        assertTrue(libraryEntry.isExported(), "the engine module must export the engine library to the project modules");

        Module projectModule = findModule(project, PROJECT + UnrealConstants.PROJECT_FILE_EXTENSION);
        Set<String> excluded = ReadAction.compute(() -> excludedUrls(projectModule));
        assertTrue(excluded.containsAll(urls(projectDirectory.resolve("Build"),
            projectDirectory.resolve("Intermediate"),
            projectDirectory.resolve("Plugins/Net/Intermediate"),
            projectDirectory.resolve("Plugins/Net/Binaries"))), "excluded: " + excluded);
    }

    /**
     * Every project open re-imports: applying the same data again must leave the project as it is, otherwise the roots
     * change rescans the whole project on every open.
     */
    @Test
    public void reimportOfTheSameProjectChangesNoRoots(HeadlessProjects projects) throws Exception {
        Path projectDirectory = createProject(projects.newDirectory());
        Path engineHome = createEngine(projects.newDirectory());
        Project project = projects.open(projectDirectory);
        HeadlessProjects.awaitIdle(project);

        importData(project, projectDirectory, engineHome);
        HeadlessProjects.awaitIdle(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            AtomicInteger rootsChanges = new AtomicInteger();
            project.getMessageBus().connect(disposable).subscribe(ModuleRootListener.class, new ModuleRootListener() {
                @Override
                public void rootsChanged(ModuleRootEvent event) {
                    rootsChanges.incrementAndGet();
                }
            });
            List<UnindexedFilesScanner> scanners = new CopyOnWriteArrayList<>();
            UnindexedFilesScannerExecutorImpl.getInstance(project)
                .setTaskFilterInTest(disposable, scanner -> {
                    scanners.add(scanner);
                    return true;
                });

            importData(project, projectDirectory, engineHome);
            HeadlessProjects.awaitIdle(project);

            assertEquals(0, rootsChanges.get(), "re-importing the same project must not change roots");
            assertEquals(List.of(), scanners, "re-importing the same project must not scan");
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    private static void importData(Project project, Path projectDirectory, Path engineHome) {
        String projectPath = projectDirectory.resolve(PROJECT + UnrealConstants.PROJECT_FILE_EXTENSION).toString();
        ProjectData projectData = new ProjectData(UnrealConstants.SYSTEM_ID, PROJECT, projectDirectory.toString(), projectPath);
        DataNode<ProjectData> projectNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);

        UnrealProjectResolver.createProjectModule(projectNode, PROJECT + UnrealConstants.PROJECT_FILE_EXTENSION, projectDirectory, projectPath);
        UnrealEngine engine = new UnrealEngine(Platform.current(), engineHome);
        DataNode<ModuleData> engineModule =
            UnrealProjectResolver.createEngineModule(projectNode, engine, projectPath, List.of(new UnrealCompileSettings()));

        Path gameSource = projectDirectory.resolve("Source").resolve(PROJECT);
        ModuleData gameModule = new ModuleData(PROJECT, UnrealConstants.SYSTEM_ID, PROJECT, gameSource.toString(), projectPath);
        DataNode<ModuleData> gameNode = projectNode.createChild(ProjectKeys.MODULE, gameModule);
        ContentRootData gameRoot = new ContentRootData(UnrealConstants.SYSTEM_ID, gameSource.toString());
        gameRoot.storePath(ExternalSystemSourceType.SOURCE, gameSource.toString());
        gameNode.createChild(ProjectKeys.CONTENT_ROOT, gameRoot);
        gameNode.createChild(ProjectKeys.MODULE_DEPENDENCY, new ModuleDependencyData(gameModule, engineModule.getData()));

        // as an import applies its data: in a write action on the UI thread, the roots changes merged into one
        ProjectDataManager dataManager = Application.get().getInstance(ProjectDataManager.class);
        ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(project) {
            @Override
            public void execute() {
                ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(
                    () -> dataManager.importData(ProjectKeys.PROJECT, List.of(projectNode), project, true));
            }
        });
    }

    private static Path createProject(Path directory) throws IOException {
        createFile(directory.resolve(PROJECT + UnrealConstants.PROJECT_FILE_EXTENSION), "{\"FileVersion\": 3}");
        createFile(directory.resolve("Source/Game/Game.cpp"), "int game;");
        createFile(directory.resolve("Build/Linux/Icon.png"), "");
        createFile(directory.resolve("Intermediate/Build/Makefile"), "");
        createFile(directory.resolve("Plugins/Net/Net.uplugin"), "{}");
        createFile(directory.resolve("Plugins/Net/Binaries/Linux/libNet.so"), "");
        createFile(directory.resolve("Plugins/Net/Intermediate/Build/Net.json"), "{}");
        return directory;
    }

    private static Path createEngine(Path home) throws IOException {
        createFile(home.resolve(UnrealEngine.BUILD_VERSION_FILE), "{\"MajorVersion\": 5, \"MinorVersion\": 8, \"PatchVersion\": 0}");
        createFile(home.resolve("Engine/Source/Runtime/Core/Public/CoreMinimal.h"), "#pragma once");
        createFile(home.resolve("Engine/Source/Runtime/Core/Private/Core.cpp"), "int core;");
        createFile(home.resolve("Engine/Source/ThirdParty/zlib/zlib.h"), "");
        createFile(home.resolve("Engine/Source/Programs/UnrealBuildTool/UnrealBuildTool.cs"), "");
        createFile(home.resolve("Engine/Source/Programs/Shared/EpicGames.Core/Log.cs"), "");
        createFile(home.resolve("Engine/Source/Programs/UnrealHeaderTool/Main.cpp"), "");
        return home;
    }

    private static void createFile(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private static Set<String> urls(Path... paths) {
        Set<String> urls = new TreeSet<>();
        for (Path path : paths) {
            urls.add(VirtualFileUtil.pathToUrl(path.toString()));
        }
        return urls;
    }

    private static Module findModule(Project project, String name) {
        Module module = ReadAction.compute(() -> ModuleManager.getInstance(project).findModuleByName(name));
        assertNotNull(module, "module " + name + " must be imported");
        return module;
    }

    private static LibraryOrderEntry findLibraryEntry(Module module, Library library) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry libraryEntry && library.equals(libraryEntry.getLibrary())) {
                return libraryEntry;
            }
        }
        return null;
    }

    private static Set<String> excludedUrls(Module module) {
        Set<String> urls = new TreeSet<>();
        for (ContentEntry entry : ModuleRootManager.getInstance(module).getContentEntries()) {
            urls.addAll(List.of(entry.getFolderUrls(type -> type instanceof ExcludedContentFolderTypeProvider)));
        }
        return urls;
    }
}
