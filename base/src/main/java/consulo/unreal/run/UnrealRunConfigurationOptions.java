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

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.unreal.UnrealConstants;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class UnrealRunConfigurationOptions extends RunConfigurationOptions {
    private String myMode = UnrealRunMode.EDITOR.name();
    private @Nullable String myTarget;
    private String myConfiguration = UnrealConstants.DEFAULT_CONFIGURATION;

    @Attribute("mode")
    public String getMode() {
        return myMode;
    }

    public void setMode(@Nullable String mode) {
        myMode = StringUtil.isEmptyOrSpaces(mode) ? UnrealRunMode.EDITOR.name() : mode;
    }

    /**
     * @return the game target of a {@link UnrealRunMode#STANDALONE} run, {@code null} for the one named after the project
     */
    @Attribute("target")
    public @Nullable String getTarget() {
        return myTarget;
    }

    public void setTarget(@Nullable String target) {
        myTarget = StringUtil.nullize(target, true);
    }

    @Attribute("configuration")
    public String getConfiguration() {
        return myConfiguration;
    }

    public void setConfiguration(@Nullable String configuration) {
        myConfiguration = StringUtil.isEmptyOrSpaces(configuration) ? UnrealConstants.DEFAULT_CONFIGURATION : configuration;
    }
}
