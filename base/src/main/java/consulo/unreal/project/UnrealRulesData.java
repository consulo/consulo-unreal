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

import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.service.project.AbstractExternalEntityData;
import consulo.unreal.UnrealConstants;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The C# build scripts of a project ({@code *.Build.cs}, {@code *.Target.cs}) as one module without a directory - a child
 * of the {@link ProjectKeys#PROJECT} node. {@link ProjectKeys#MODULE} cannot express it: a module from there always has
 * a directory and its content roots are directories, while the scripts are single files spread over the C++ modules.
 *
 * @param moduleName   name of the module, e.g. {@code Brick2D.Rules}
 * @param files        the scripts, each becoming a single-file content entry
 * @param extensionIds module extensions to enable, parents first; missing ones (optional part not loaded) are skipped
 * @param engineHome   the engine whose bundle the extensions get - its UnrealBuildTool and .NET the scripts use
 * @param dependencies modules the scripts depend on - the engine module, which holds the UnrealBuildTool sources
 */
public class UnrealRulesData extends AbstractExternalEntityData {
    private static final long serialVersionUID = 1L;

    /**
     * Processed after the regular modules, so the project structure is in place.
     */
    public static final Key<UnrealRulesData> KEY = Key.create(UnrealRulesData.class, ProjectKeys.CONTENT_ROOT.getProcessingWeight() + 2);

    private final String myModuleName;
    private final List<String> myFiles;
    private final List<String> myExtensionIds;
    private final @Nullable String myEngineHome;
    private final List<String> myDependencies;

    public UnrealRulesData(String moduleName,
                           List<String> files,
                           List<String> extensionIds,
                           @Nullable String engineHome,
                           List<String> dependencies) {
        super(UnrealConstants.SYSTEM_ID);
        myModuleName = moduleName;
        myFiles = List.copyOf(files);
        myExtensionIds = List.copyOf(extensionIds);
        myEngineHome = engineHome;
        myDependencies = List.copyOf(dependencies);
    }

    public String getModuleName() {
        return myModuleName;
    }

    public List<String> getFiles() {
        return myFiles;
    }

    public List<String> getExtensionIds() {
        return myExtensionIds;
    }

    public @Nullable String getEngineHome() {
        return myEngineHome;
    }

    public List<String> getDependencies() {
        return myDependencies;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof UnrealRulesData that) || !super.equals(o)) {
            return false;
        }
        return myModuleName.equals(that.myModuleName)
            && myFiles.equals(that.myFiles)
            && myExtensionIds.equals(that.myExtensionIds)
            && Objects.equals(myEngineHome, that.myEngineHome)
            && myDependencies.equals(that.myDependencies);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(myModuleName, myFiles, myExtensionIds, myEngineHome, myDependencies);
    }
}
