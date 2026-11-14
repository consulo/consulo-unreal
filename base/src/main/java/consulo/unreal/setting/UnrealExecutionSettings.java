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

import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * What the resolver and the task manager need to run UnrealBuildTool for a project - resolved on the IDE side, because
 * the engine is a bundle of the IDE.
 */
public class UnrealExecutionSettings extends ExternalSystemExecutionSettings {
    private final @Nullable String myEngineHome;
    private final @Nullable String myTarget;
    private final String myConfiguration;
    private final boolean myIndexEngineSources;

    public UnrealExecutionSettings(@Nullable String engineHome, @Nullable String target, String configuration, boolean indexEngineSources) {
        myEngineHome = engineHome;
        myTarget = target;
        myConfiguration = configuration;
        myIndexEngineSources = indexEngineSources;
    }

    public @Nullable String getEngineHome() {
        return myEngineHome;
    }

    public @Nullable String getTarget() {
        return myTarget;
    }

    public String getConfiguration() {
        return myConfiguration;
    }

    public boolean isIndexEngineSources() {
        return myIndexEngineSources;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(myEngineHome, myTarget, myConfiguration, myIndexEngineSources);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof UnrealExecutionSettings that) || !super.equals(o)) {
            return false;
        }
        return Objects.equals(myEngineHome, that.myEngineHome)
            && Objects.equals(myTarget, that.myTarget)
            && myConfiguration.equals(that.myConfiguration)
            && myIndexEngineSources == that.myIndexEngineSources;
    }
}
