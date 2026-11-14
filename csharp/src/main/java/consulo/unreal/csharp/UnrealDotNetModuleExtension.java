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

import consulo.content.bundle.SdkType;
import consulo.dotnet.module.extension.BaseDotNetSimpleModuleExtension;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.unreal.UnrealConstants;
import consulo.unreal.engine.UnrealEngine;
import consulo.unreal.engine.UnrealEngineSdkType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * .NET of the build scripts: the engine is the bundle, its system libraries are the .NET assemblies the engine bundles
 * (see {@link UnrealEngine#findDotNetAssemblies()}) and UnrealBuildTool - the ones UnrealBuildTool compiles them with.
 */
public class UnrealDotNetModuleExtension extends BaseDotNetSimpleModuleExtension<UnrealDotNetModuleExtension> {
    public UnrealDotNetModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @Override
    public Class<? extends SdkType> getSdkTypeClass() {
        return UnrealEngineSdkType.class;
    }

    @Override
    public boolean isSupportCompilation() {
        // UnrealBuildTool compiles the scripts itself
        return false;
    }

    /**
     * The {@link UnrealConstants#RULES_REFERENCES} - from the .NET runtime the engine bundles, else from the
     * UnrealBuildTool directory.
     */
    @Override
    public File[] getFilesForLibraries() {
        UnrealEngine engine = UnrealEngine.fromSdk(getSdk());
        if (engine == null) {
            return new File[0];
        }
        List<Path> directories = new ArrayList<>();
        Path dotNetAssemblies = engine.findDotNetAssemblies();
        if (dotNetAssemblies != null) {
            directories.add(dotNetAssemblies);
        }
        directories.add(engine.getBuildToolDirectory());

        List<File> files = new ArrayList<>();
        for (String assemblyName : UnrealConstants.RULES_REFERENCES) {
            for (Path directory : directories) {
                Path assembly = directory.resolve(assemblyName + ".dll");
                if (Files.isRegularFile(assembly)) {
                    files.add(assembly.toFile());
                    break;
                }
            }
        }
        return files.toArray(new File[0]);
    }
}
