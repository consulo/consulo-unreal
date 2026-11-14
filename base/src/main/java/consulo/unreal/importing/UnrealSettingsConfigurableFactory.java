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
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.setting.AbstractExternalProjectSettingsConfigurable;
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurableFactory;
import consulo.externalSystem.service.setting.ExternalSystemSettingsPlace;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.unreal.UnrealConstants;
import consulo.unreal.setting.UnrealProjectSettings;
import consulo.unreal.setting.UnrealSettings;

@ExtensionImpl
public class UnrealSettingsConfigurableFactory implements ExternalSystemSettingsConfigurableFactory<UnrealProjectSettings, UnrealSettings> {
    @Override
    public ProjectSystemId getSystemId() {
        return UnrealConstants.SYSTEM_ID;
    }

    @Override
    public UnrealProjectSettings createProjectSettings() {
        return new UnrealProjectSettings();
    }

    @Override
    public UnrealSettings createSystemSettings() {
        return new UnrealSettings(ProjectManager.getInstance().getDefaultProject());
    }

    @Override
    @RequiredUIAccess
    public AbstractExternalProjectSettingsConfigurable<UnrealProjectSettings> createProjectSettingsConfigurable(
        UnrealProjectSettings settings,
        ExternalSystemSettingsPlace place
    ) {
        return new UnrealProjectSettingsConfigurable(settings, place);
    }
}
