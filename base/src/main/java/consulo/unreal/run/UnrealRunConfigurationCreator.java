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

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.unreal.localize.UnrealLocalize;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates "Editor" and "Game" run configurations of an imported project, once - existing ones (also edited or removed by
 * the user, as long as one of the mode remains) are kept.
 */
public final class UnrealRunConfigurationCreator {
    private UnrealRunConfigurationCreator() {
    }

    /**
     * @param module the module of the project file ({@code <Project>.uproject})
     */
    @RequiredUIAccess
    public static void createConfigurations(Project project, Module module, String projectName) {
        if (project.isDisposed() || module.isDisposed()) {
            return;
        }
        UnrealRunConfigurationType type = UnrealRunConfigurationType.getInstance(project.getApplication());
        RunManager runManager = RunManager.getInstance(project);
        Set<UnrealRunMode> existing = new HashSet<>();
        for (RunConfiguration configuration : runManager.getConfigurationsList(type)) {
            if (configuration instanceof UnrealRunConfiguration unreal && module.getName().equals(unreal.getConfigurationModule().getModuleName())) {
                existing.add(unreal.getMode());
            }
        }

        ConfigurationFactory factory = type.getConfigurationFactories()[0];
        RunnerAndConfigurationSettings first = null;
        for (UnrealRunMode mode : new UnrealRunMode[]{UnrealRunMode.EDITOR, UnrealRunMode.GAME}) {
            if (existing.contains(mode)) {
                continue;
            }
            String name = mode == UnrealRunMode.EDITOR
                ? UnrealLocalize.runConfigurationEditorName(projectName).get()
                : UnrealLocalize.runConfigurationGameName(projectName).get();
            RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(name, factory);
            UnrealRunConfiguration configuration = (UnrealRunConfiguration) settings.getConfiguration();
            configuration.setModule(module);
            configuration.setMode(mode);
            runManager.addConfiguration(settings, false);
            if (first == null) {
                first = settings;
            }
        }
        if (first != null && runManager.getSelectedConfiguration() == null) {
            runManager.setSelectedConfiguration(first);
        }
    }
}
