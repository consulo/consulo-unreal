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

package consulo.unreal.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.content.layer.ModifiableRootModel;

/**
 * Completes the module of the build scripts after its files are set and its extensions are enabled - e.g. the C# support
 * adds the assemblies the scripts compile against. Implemented by the optional parts of the plugin.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface UnrealRulesModuleContributor {
    void contribute(ModifiableRootModel rootModel, UnrealRulesData data);
}
