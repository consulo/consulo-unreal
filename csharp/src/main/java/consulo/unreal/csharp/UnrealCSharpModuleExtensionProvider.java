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

package consulo.unreal.csharp;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;
import consulo.unreal.UnrealConstants;
import consulo.unreal.icon.UnrealIconGroup;
import consulo.unreal.localize.UnrealLocalize;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class UnrealCSharpModuleExtensionProvider implements ModuleExtensionProvider<UnrealCSharpModuleExtension> {
    @Override
    public String getId() {
        return UnrealConstants.RULES_CSHARP_EXTENSION_ID;
    }

    @Override
    public @Nullable String getParentId() {
        return UnrealConstants.RULES_DOTNET_EXTENSION_ID;
    }

    @Override
    public boolean isSystemOnly() {
        return true;
    }

    @Override
    public LocalizeValue getName() {
        return UnrealLocalize.rulesCsharpExtensionName();
    }

    @Override
    public Image getIcon() {
        return UnrealIconGroup.unreal();
    }

    @Override
    public ModuleExtension<UnrealCSharpModuleExtension> createImmutableExtension(ModuleRootLayer layer) {
        return new UnrealCSharpModuleExtension(getId(), layer);
    }

    @Override
    public MutableModuleExtension<UnrealCSharpModuleExtension> createMutableExtension(ModuleRootLayer layer) {
        return new UnrealCSharpMutableModuleExtension(getId(), layer);
    }
}
