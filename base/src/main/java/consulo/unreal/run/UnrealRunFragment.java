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

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.unreal.UnrealConstants;
import consulo.unreal.localize.UnrealLocalize;

import java.util.List;

/**
 * What to start - editor, game or a built target - and, for a target, which one in which configuration.
 */
public class UnrealRunFragment implements RunConfigurationFragment<UnrealRunConfiguration> {
    @Override
    public String getId() {
        return "unrealRun";
    }

    @Override
    public LocalizeValue getName() {
        return UnrealLocalize.runConfigurationFragmentName();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.FIRST;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<UnrealRunConfiguration> createEditor(Project project, Disposable parent) {
        ComboBox<UnrealRunMode> modeBox = ComboBox.create(List.of(UnrealRunMode.values()));
        modeBox.setRenderer((presentation, item) -> {
            UnrealRunMode mode = item.getValue();
            if (mode != null) {
                presentation.append(mode.getPresentableName());
            }
        });

        TextBox targetBox = TextBox.create();
        targetBox.setPlaceholder(UnrealLocalize.runConfigurationTargetPlaceholder());

        ComboBox<String> configurationBox = ComboBox.create(UnrealConstants.CONFIGURATIONS);

        Runnable updateEnabled = () -> {
            boolean standalone = modeBox.getValue() == UnrealRunMode.STANDALONE;
            targetBox.setEnabled(standalone);
            configurationBox.setEnabled(standalone);
        };
        modeBox.addValueListener(event -> updateEnabled.run());

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(UnrealLocalize.runConfigurationModeLabel(), modeBox);
                layout.addLabeled(UnrealLocalize.runConfigurationTargetLabel(), targetBox);
                layout.addLabeled(UnrealLocalize.runConfigurationConfigurationLabel(), configurationBox);
            }

            @RequiredUIAccess
            @Override
            public void reset(UnrealRunConfiguration configuration) {
                modeBox.setValue(configuration.getMode());
                String target = configuration.getOptions().getTarget();
                targetBox.setValue(target == null ? "" : target);
                configurationBox.setValue(configuration.getOptions().getConfiguration());
                updateEnabled.run();
            }

            @RequiredUIAccess
            @Override
            public void apply(UnrealRunConfiguration configuration) {
                UnrealRunMode mode = modeBox.getValue();
                configuration.setMode(mode == null ? UnrealRunMode.EDITOR : mode);
                configuration.getOptions().setTarget(targetBox.getValue());
                configuration.getOptions().setConfiguration(configurationBox.getValue());
            }
        };
    }
}
