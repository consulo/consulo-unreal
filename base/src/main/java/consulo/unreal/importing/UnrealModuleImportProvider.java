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

package consulo.unreal.importing;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.importing.AbstractExternalModuleImportProvider;
import consulo.externalSystem.importing.ExternalModuleImportContext;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.unreal.UnrealConstants;
import consulo.unreal.UnrealUtil;
import consulo.unreal.icon.UnrealIconGroup;
import jakarta.inject.Inject;

import java.io.File;
import java.nio.file.Path;

/**
 * Opens/imports a {@code .uproject} or the directory holding one. The project file is the linked project path.
 */
@ExtensionImpl
public class UnrealModuleImportProvider extends AbstractExternalModuleImportProvider {
    @Inject
    public UnrealModuleImportProvider(ProjectDataManager dataManager) {
        super(dataManager, UnrealConstants.SYSTEM_ID);
    }

    @Override
    public Image getIcon() {
        return UnrealIconGroup.unreal();
    }

    @Override
    public boolean canImport(Path fileOrDirectory) {
        return UnrealUtil.findProjectFile(fileOrDirectory) != null;
    }

    @Override
    protected File getExternalProjectConfigToUse(File file) {
        Path projectFile = UnrealUtil.findProjectFile(file.toPath());
        return projectFile == null ? file : projectFile.toFile();
    }

    @Override
    protected void doPrepare(ExternalModuleImportContext context) {
    }

    @Override
    protected void beforeCommit(DataNode<ProjectData> dataNode, Project project) {
    }

    @Override
    protected void applyExtraSettings(ExternalModuleImportContext context) {
    }
}
