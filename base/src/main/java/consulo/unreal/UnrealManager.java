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

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.service.project.autoimport.CachingExternalSystemAutoImportAware;
import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.unreal.project.UnrealProjectResolver;
import consulo.unreal.setting.UnrealExecutionSettings;
import consulo.unreal.setting.UnrealLocalSettings;
import consulo.unreal.setting.UnrealProjectSettings;
import consulo.unreal.setting.UnrealSettings;
import consulo.unreal.setting.UnrealSettingsListener;
import consulo.unreal.task.UnrealTaskManager;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@ExtensionImpl
public class UnrealManager implements ExternalSystemAutoImportAware,
    ExternalSystemManager<UnrealProjectSettings, UnrealSettingsListener, UnrealSettings, UnrealLocalSettings, UnrealExecutionSettings> {

    private final UnrealAutoImportAware myAutoImportAware = new UnrealAutoImportAware();
    private final ExternalSystemAutoImportAware myAutoImportDelegate = new CachingExternalSystemAutoImportAware(myAutoImportAware);

    @Override
    public ProjectSystemId getSystemId() {
        return UnrealConstants.SYSTEM_ID;
    }

    @Override
    public Function<Project, UnrealSettings> getSettingsProvider() {
        return UnrealSettings::getInstance;
    }

    @Override
    public Function<Project, UnrealLocalSettings> getLocalSettingsProvider() {
        return UnrealLocalSettings::getInstance;
    }

    @Override
    public Function<Pair<Project, String>, UnrealExecutionSettings> getExecutionSettingsProvider() {
        return pair -> UnrealUtil.createExecutionSettings(pair.getFirst(), pair.getSecond());
    }

    @Override
    public Supplier<? extends ExternalSystemProjectResolver<UnrealExecutionSettings>> getProjectResolverFactory() {
        return UnrealProjectResolver::new;
    }

    @Override
    public Supplier<? extends ExternalSystemTaskManager<UnrealExecutionSettings>> getTaskManagerFactory() {
        return UnrealTaskManager::new;
    }

    @Override
    public FileChooserDescriptor getExternalProjectDescriptor() {
        return new FileChooserDescriptor(true, true, false, false, false, false) {
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return file.isDirectory() || file.getName().endsWith(UnrealConstants.PROJECT_FILE_EXTENSION);
            }
        };
    }

    @Override
    public @Nullable String getAffectedExternalProjectPath(String changedFileOrDirPath, Project project) {
        return myAutoImportDelegate.getAffectedExternalProjectPath(changedFileOrDirPath, project);
    }

    @Override
    public List<Path> getAffectedExternalProjectFilePaths(String projectPath, Project project) {
        return myAutoImportAware.getAffectedExternalProjectFilePaths(projectPath, project);
    }
}
