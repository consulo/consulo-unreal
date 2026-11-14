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

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.disposer.Disposable;
import consulo.externalSystem.service.setting.AbstractExternalProjectSettingsConfigurable;
import consulo.externalSystem.service.setting.ExternalSystemSettingsPlace;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.unreal.UnrealConstants;
import consulo.unreal.engine.UnrealEngineSdkType;
import consulo.unreal.localize.UnrealLocalize;
import consulo.unreal.setting.UnrealProjectSettings;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class UnrealProjectSettingsConfigurable extends AbstractExternalProjectSettingsConfigurable<UnrealProjectSettings> {
    private static final String ASSOCIATED_ENGINE = "";

    private @Nullable ComboBox<String> myEngineBox;
    private @Nullable TextBox myTargetBox;
    private @Nullable ComboBox<String> myConfigurationBox;
    private @Nullable CheckBox myIndexEngineSourcesBox;

    public UnrealProjectSettingsConfigurable(UnrealProjectSettings settings, ExternalSystemSettingsPlace place) {
        super(settings, place);
    }

    @Override
    @RequiredUIAccess
    protected Component createExtraUIComponent(Disposable uiDisposable) {
        ComboBox.Builder<String> engines = ComboBox.builder();
        engines.add(ASSOCIATED_ENGINE, UnrealLocalize.settingsEngineAuto());
        for (Sdk sdk : SdkTable.getInstance().getSdksOfType(UnrealEngineSdkType.getInstance())) {
            engines.add(sdk.getName(), LocalizeValue.of(sdk.getName()));
        }
        ComboBox<String> engineBox = engines.build();
        String engineName = getSettings().getEngineName();
        engineBox.setValue(engineName == null ? ASSOCIATED_ENGINE : engineName);
        myEngineBox = engineBox;

        String target = getSettings().getTarget();
        TextBox targetBox = TextBox.create(target == null ? "" : target);
        targetBox.setPlaceholder(UnrealLocalize.settingsTargetAuto());
        myTargetBox = targetBox;

        ComboBox<String> configurationBox = ComboBox.create(UnrealConstants.CONFIGURATIONS);
        configurationBox.setValue(getSettings().getConfiguration());
        myConfigurationBox = configurationBox;

        CheckBox indexEngineSourcesBox = CheckBox.create(UnrealLocalize.settingsIndexEngineLabel(), getSettings().isIndexEngineSources());
        myIndexEngineSourcesBox = indexEngineSourcesBox;

        FormBuilder builder = FormBuilder.create()
            .addLabeled(UnrealLocalize.settingsEngineLabel(), engineBox)
            .addLabeled(UnrealLocalize.settingsTargetLabel(), targetBox)
            .addLabeled(UnrealLocalize.settingsConfigurationLabel(), configurationBox);
        builder.addBottom(indexEngineSourcesBox);
        return builder.build();
    }

    @Override
    @RequiredUIAccess
    protected boolean isExtraModified() {
        return !Objects.equals(getEngineName(), getSettings().getEngineName())
            || !Objects.equals(getTarget(), getSettings().getTarget())
            || !getConfiguration().equals(getSettings().getConfiguration())
            || isIndexEngineSources() != getSettings().isIndexEngineSources();
    }

    @Override
    @RequiredUIAccess
    protected void applyExtra() {
        if (myEngineBox == null) {
            return;
        }
        getSettings().setEngineName(getEngineName());
        getSettings().setTarget(getTarget());
        getSettings().setConfiguration(getConfiguration());
        getSettings().setIndexEngineSources(isIndexEngineSources());
    }

    @RequiredUIAccess
    private @Nullable String getEngineName() {
        String value = myEngineBox == null ? null : myEngineBox.getValue();
        return value == null || ASSOCIATED_ENGINE.equals(value) ? null : value;
    }

    @RequiredUIAccess
    private @Nullable String getTarget() {
        String value = myTargetBox == null ? null : myTargetBox.getValue();
        return value == null || value.isBlank() ? null : value.trim();
    }

    @RequiredUIAccess
    private String getConfiguration() {
        String value = myConfigurationBox == null ? null : myConfigurationBox.getValue();
        return value == null ? UnrealConstants.DEFAULT_CONFIGURATION : value;
    }

    @RequiredUIAccess
    private boolean isIndexEngineSources() {
        return myIndexEngineSourcesBox == null || myIndexEngineSourcesBox.getValueOrError();
    }
}
