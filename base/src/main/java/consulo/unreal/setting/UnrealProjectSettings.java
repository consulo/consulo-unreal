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

package consulo.unreal.setting;

import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.unreal.UnrealConstants;
import org.jspecify.annotations.Nullable;

/**
 * Settings of one linked project - its {@code .uproject} file.
 */
public class UnrealProjectSettings extends ExternalProjectSettings {
    private @Nullable String myEngineName;
    private @Nullable String myTarget;
    private String myConfiguration = UnrealConstants.DEFAULT_CONFIGURATION;
    private boolean myIndexEngineSources = true;

    /**
     * @return name of the engine bundle, {@code null} for the engine the project is associated with
     */
    public @Nullable String getEngineName() {
        return myEngineName;
    }

    public void setEngineName(@Nullable String engineName) {
        myEngineName = engineName;
    }

    /**
     * @return UnrealBuildTool target to import and build, {@code null} for the editor target of the project
     */
    public @Nullable String getTarget() {
        return myTarget;
    }

    public void setTarget(@Nullable String target) {
        myTarget = target == null || target.isBlank() ? null : target.trim();
    }

    public String getConfiguration() {
        return myConfiguration;
    }

    public void setConfiguration(@Nullable String configuration) {
        myConfiguration = configuration == null || configuration.isBlank() ? UnrealConstants.DEFAULT_CONFIGURATION : configuration;
    }

    /**
     * @return whether {@code Engine/Source} becomes a module, so engine headers are indexed and resolve their own includes
     */
    public boolean isIndexEngineSources() {
        return myIndexEngineSources;
    }

    public void setIndexEngineSources(boolean indexEngineSources) {
        myIndexEngineSources = indexEngineSources;
    }

    @Override
    public UnrealProjectSettings clone() {
        UnrealProjectSettings clone = new UnrealProjectSettings();
        copyTo(clone);
        clone.myEngineName = myEngineName;
        clone.myTarget = myTarget;
        clone.myConfiguration = myConfiguration;
        clone.myIndexEngineSources = myIndexEngineSources;
        return clone;
    }
}
