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
import consulo.content.bundle.Sdk;
import consulo.dotnet.impl.roots.orderEntry.DotNetLibraryOrderEntryType;
import consulo.dotnet.module.extension.DotNetLibraryOrderEntryModel;
import consulo.dotnet.module.extension.DotNetSimpleModuleExtension;
import consulo.dotnet.module.extension.DotNetSimpleMutableModuleExtension;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.unreal.engine.UnrealEngineSdkType;
import consulo.unreal.project.UnrealRulesData;
import consulo.unreal.project.UnrealRulesModuleContributor;

import java.nio.file.Path;

/**
 * Makes every system library of the engine bundle - the .NET reference assemblies and UnrealBuildTool - a dependency of
 * the build scripts, as UnrealBuildTool references them all when it compiles the rules assembly.
 */
@ExtensionImpl
public class UnrealDotNetRulesContributor implements UnrealRulesModuleContributor {
    @Override
    public void contribute(ModifiableRootModel rootModel, UnrealRulesData data) {
        DotNetSimpleModuleExtension<?> extension = rootModel.getExtension(DotNetSimpleModuleExtension.class);
        if (!(extension instanceof DotNetSimpleMutableModuleExtension<?> mutableExtension)) {
            return;
        }
        // the .NET extensions keep their bundle in a pointer of their own, not in ModuleExtensionWithSdk
        String engineHome = data.getEngineHome();
        Sdk engineSdk = engineHome == null ? null : UnrealEngineSdkType.findOrCreateSdk(Path.of(engineHome));
        if (engineSdk != null) {
            mutableExtension.getInheritableSdk().set(null, engineSdk.getName());
        }

        DotNetLibraryOrderEntryType type = DotNetLibraryOrderEntryType.getInstance();
        for (OrderEntry entry : rootModel.getOrderEntries()) {
            if (entry.getType() == type) {
                rootModel.removeOrderEntry(entry);
            }
        }
        for (String assemblyName : mutableExtension.getAvailableSystemLibraries().keySet()) {
            rootModel.addCustomOderEntry(type, new DotNetLibraryOrderEntryModel(assemblyName));
        }
    }
}
