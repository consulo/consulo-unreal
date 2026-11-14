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
import consulo.cpp.lang.version.CLanguageVersions;
import consulo.cpp.module.extension.CCompileSettings;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.unreal.module.UnrealCMutableModuleExtension;

import java.util.Collection;

/**
 * Turns the Unreal C/C++ extension of an imported module on and gives it the include paths and defines of the module.
 */
@ExtensionImpl
public class UnrealCompileDataService implements ProjectDataService<UnrealCompileData, Module> {
    @Override
    public Key<UnrealCompileData> getTargetDataKey() {
        return UnrealCompileData.KEY;
    }

    @Override
    public void importData(Collection<DataNode<UnrealCompileData>> toImport, Project project, boolean synchronous) {
        if (toImport.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
            @Override
            public void execute() {
                ModuleManager moduleManager = ModuleManager.getInstance(project);
                for (DataNode<UnrealCompileData> node : toImport) {
                    ModuleData moduleData = node.getData(ProjectKeys.MODULE);
                    Module module = moduleData == null ? null : moduleManager.findModuleByName(moduleData.getInternalName());
                    if (module != null && !module.isDisposed()) {
                        apply(module, node.getData());
                    }
                }
            }
        });
    }

    @Override
    public void removeData(Collection<? extends Module> toRemove, Project project, boolean synchronous) {
    }

    private static void apply(Module module, UnrealCompileData data) {
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        UnrealCMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(UnrealCMutableModuleExtension.class);
        if (extension == null) {
            rootModel.dispose();
            return;
        }
        CCompileSettings settings = new CCompileSettings();
        settings.setIncludePaths(data.getIncludePaths());
        settings.setDefines(data.getDefines());
        settings.setCppStandard(CLanguageVersions.findById(data.getCppStandardId()));
        extension.setEnabled(true);
        extension.setCompileSettings(settings);
        rootModel.commit();
    }
}
