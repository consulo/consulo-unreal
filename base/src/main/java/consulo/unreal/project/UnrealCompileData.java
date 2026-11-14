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
import java.util.Map;
import java.util.Objects;

/**
 * C++ settings of an imported module - a child of its {@link ProjectKeys#MODULE} node, applied to the module's
 * {@link consulo.unreal.module.UnrealCModuleExtension}.
 */
public class UnrealCompileData extends AbstractExternalEntityData {
    private static final long serialVersionUID = 1L;

    /**
     * Processed after the module and its content roots exist.
     */
    public static final Key<UnrealCompileData> KEY = Key.create(UnrealCompileData.class, ProjectKeys.CONTENT_ROOT.getProcessingWeight() + 1);

    private final List<String> myIncludePaths;
    private final Map<String, String> myDefines;
    private final @Nullable String myCppStandardId;

    /**
     * @param cppStandardId id of a {@link consulo.cpp.lang.version.CLanguageVersion}, e.g. {@code CPP20}
     */
    public UnrealCompileData(List<String> includePaths, Map<String, String> defines, @Nullable String cppStandardId) {
        super(UnrealConstants.SYSTEM_ID);
        myIncludePaths = List.copyOf(includePaths);
        myDefines = Map.copyOf(defines);
        myCppStandardId = cppStandardId;
    }

    public List<String> getIncludePaths() {
        return myIncludePaths;
    }

    public Map<String, String> getDefines() {
        return myDefines;
    }

    public @Nullable String getCppStandardId() {
        return myCppStandardId;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof UnrealCompileData that) || !super.equals(o)) {
            return false;
        }
        return myIncludePaths.equals(that.myIncludePaths)
            && myDefines.equals(that.myDefines)
            && Objects.equals(myCppStandardId, that.myCppStandardId);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(myIncludePaths, myDefines, myCppStandardId);
    }
}
