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
import consulo.application.Application;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationModule;
import consulo.project.Project;
import consulo.unreal.icon.UnrealIconGroup;
import consulo.unreal.localize.UnrealLocalize;

@ExtensionImpl
public class UnrealRunConfigurationType extends ConfigurationTypeBase {
    public static UnrealRunConfigurationType getInstance(Application application) {
        return application.getExtensionPoint(ConfigurationType.class).findExtensionOrFail(UnrealRunConfigurationType.class);
    }

    public UnrealRunConfigurationType() {
        super("UnrealEngine", UnrealLocalize.runConfigurationTypeName(), UnrealLocalize.runConfigurationTypeDescription(), UnrealIconGroup.unreal());
        addFactory(new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new UnrealRunConfiguration("", new RunConfigurationModule(project), this);
            }
        });
    }
}
