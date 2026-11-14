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
import consulo.application.Application;
import consulo.content.bundle.Sdk;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.project.Project;
import consulo.unreal.engine.UnrealEngineSdkType;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Creates the module of the build scripts itself: without a directory, with every script as a single-file content entry,
 * its extensions bound to the engine bundle, and completed by the {@link UnrealRulesModuleContributor}s.
 */
@ExtensionImpl
public class UnrealRulesDataService implements ProjectDataService<UnrealRulesData, Module> {
    @Override
    public Key<UnrealRulesData> getTargetDataKey() {
        return UnrealRulesData.KEY;
    }

    @Override
    public void importData(Collection<DataNode<UnrealRulesData>> toImport, Project project, boolean synchronous) {
        if (toImport.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
            @Override
            public void execute() {
                for (DataNode<UnrealRulesData> node : toImport) {
                    importRules(project, node.getData());
                }
            }
        });
    }

    @Override
    public void removeData(Collection<? extends Module> toRemove, Project project, boolean synchronous) {
    }

    private static void importRules(Project project, UnrealRulesData data) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module module = moduleManager.findModuleByName(data.getModuleName());
        if (module == null) {
            ModifiableModuleModel moduleModel = moduleManager.getModifiableModel();
            module = moduleModel.newModule(data.getModuleName(), (String) null);
            moduleModel.commit();
        }

        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        for (ContentEntry entry : rootModel.getContentEntries()) {
            rootModel.removeContentEntry(entry);
        }
        for (String file : data.getFiles()) {
            rootModel.addSingleContentEntry(VirtualFileUtil.pathToUrl(file));
        }

        for (OrderEntry entry : rootModel.getOrderEntries()) {
            if (entry instanceof ModuleOrderEntry) {
                rootModel.removeOrderEntry(entry);
            }
        }
        for (String dependency : data.getDependencies()) {
            Module dependencyModule = moduleManager.findModuleByName(dependency);
            if (dependencyModule != null) {
                rootModel.addModuleOrderEntry(dependencyModule);
            }
        }

        String engineHome = data.getEngineHome();
        Sdk engineSdk = engineHome == null ? null : UnrealEngineSdkType.findOrCreateSdk(Path.of(engineHome));
        for (String extensionId : data.getExtensionIds()) {
            MutableModuleExtension<?> extension = rootModel.getExtensionWithoutCheck(extensionId);
            if (extension == null) {
                // provided by an optional part of the plugin which is not loaded
                continue;
            }
            extension.setEnabled(true);
            // extensions with a bundle of their own kind (e.g. the .NET ones) bind it in their contributor
            if (engineSdk != null
                && extension instanceof ModuleExtensionWithSdk<?> extensionWithSdk
                && extensionWithSdk.getInheritableSdk() instanceof MutableModuleInheritableNamedPointer<Sdk> sdkPointer) {
                sdkPointer.set(null, engineSdk.getName());
                if (rootModel.findModuleExtensionSdkEntry(extensionWithSdk) == null) {
                    rootModel.addModuleExtensionSdkEntry(extensionWithSdk);
                }
            }
        }

        Application.get().getExtensionPoint(UnrealRulesModuleContributor.class)
            .forEach(contributor -> contributor.contribute(rootModel, data));
        rootModel.commit();
    }
}
