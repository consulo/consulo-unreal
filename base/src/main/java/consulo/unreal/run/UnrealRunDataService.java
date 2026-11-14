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

package consulo.unreal.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;

import java.util.Collection;

@ExtensionImpl
public class UnrealRunDataService implements ProjectDataService<UnrealRunData, Module> {
    private static final Logger LOG = Logger.getInstance(UnrealRunDataService.class);

    @Override
    public Key<UnrealRunData> getTargetDataKey() {
        return UnrealRunData.KEY;
    }

    @Override
    public void importData(Collection<DataNode<UnrealRunData>> toImport, Project project, boolean synchronous) {
        LOG.info("Run data to import: " + toImport.size());
        for (DataNode<UnrealRunData> node : toImport) {
            UnrealRunData data = node.getData();
            project.getUIAccess().give(() -> {
                Module module = ModuleManager.getInstance(project).findModuleByName(data.getProjectModuleName());
                LOG.info("Run configurations for " + data.getProjectModuleName() + ": module " + module);
                if (module != null) {
                    UnrealRunConfigurationCreator.createConfigurations(project, module, data.getProjectName());
                }
            });
        }
    }

    @Override
    public void removeData(Collection<? extends Module> toRemove, Project project, boolean synchronous) {
    }
}
