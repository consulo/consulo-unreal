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

import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Creates the engine library of {@link UnrealEngineLibraryData} and adds it to its modules. Every import runs on project
 * open, so the library and the modules are changed only when they differ - an unchanged commit would rescan the project.
 */
@ExtensionImpl
public class UnrealEngineLibraryDataService implements ProjectDataService<UnrealEngineLibraryData, Library> {
    /**
     * Headers are indexed as library classes, like the system headers of other C/C++ imports; the source root makes
     * navigation open them as sources.
     */
    private static final List<String> ROOT_TYPES = List.of(BinariesOrderRootType.ID, SourcesOrderRootType.ID);

    @Override
    public Key<UnrealEngineLibraryData> getTargetDataKey() {
        return UnrealEngineLibraryData.KEY;
    }

    @Override
    public void importData(Collection<DataNode<UnrealEngineLibraryData>> toImport, Project project, boolean synchronous) {
        if (toImport.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
            @Override
            public void execute() {
                for (DataNode<UnrealEngineLibraryData> node : toImport) {
                    Library library = importLibrary(project, node.getData());
                    addToModules(project, library, node.getData().getModuleNames());
                }
            }
        });
    }

    @Override
    public void removeData(Collection<? extends Library> toRemove, Project project, boolean synchronous) {
    }

    private static Library importLibrary(Project project, UnrealEngineLibraryData data) {
        LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
        Library library = libraryTable.getLibraryByName(data.getLibraryName());
        if (library == null) {
            LibraryTable.ModifiableModel tableModel = libraryTable.getModifiableModel();
            library = tableModel.createLibrary(data.getLibraryName());
            tableModel.commit();
        }

        String rootUrl = VirtualFileUtil.pathToUrl(data.getRoot());
        Set<String> excludedUrls = new TreeSet<>();
        for (String excluded : data.getExcludedRoots()) {
            excludedUrls.add(VirtualFileUtil.pathToUrl(excluded));
        }
        if (isUpToDate(library, rootUrl, excludedUrls)) {
            return library;
        }

        Library.ModifiableModel libraryModel = library.getModifiableModel();
        for (String rootType : ROOT_TYPES) {
            for (String url : libraryModel.getUrls(rootType)) {
                libraryModel.removeRoot(url, rootType);
            }
            libraryModel.addRoot(rootUrl, rootType);
        }
        for (String url : libraryModel.getExcludedRootUrls()) {
            libraryModel.removeExcludedRoot(url);
        }
        for (String url : excludedUrls) {
            libraryModel.addExcludedRoot(url);
        }
        libraryModel.commit();
        return library;
    }

    private static boolean isUpToDate(Library library, String rootUrl, Set<String> excludedUrls) {
        for (String rootType : ROOT_TYPES) {
            if (!List.of(library.getUrls(rootType)).equals(List.of(rootUrl))) {
                return false;
            }
        }
        return new TreeSet<>(List.of(library.getExcludedRootUrls())).equals(excludedUrls);
    }

    private static void addToModules(Project project, Library library, List<String> moduleNames) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (String moduleName : moduleNames) {
            Module module = moduleManager.findModuleByName(moduleName);
            if (module == null || module.isDisposed()) {
                continue;
            }
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            LibraryOrderEntry entry = rootModel.findLibraryOrderEntry(library);
            if (entry == null) {
                entry = rootModel.addLibraryEntry(library);
            }
            // the modules depending on this one reach the engine headers through it
            if (!entry.isExported()) {
                entry.setExported(true);
            }
            if (rootModel.isChanged()) {
                rootModel.commit();
            }
            else {
                rootModel.dispose();
            }
        }
    }
}
