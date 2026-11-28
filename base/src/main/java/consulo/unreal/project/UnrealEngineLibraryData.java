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
 * The C++ sources of the engine as a project library - a child of the {@link ProjectKeys#PROJECT} node.
 * {@link ProjectKeys#LIBRARY} cannot express it: its data has no excluded roots, and the engine sources are useful only
 * without their implementation and third party code.
 *
 * @param libraryName   e.g. {@code UE 5.8.4 Sources}
 * @param root          {@code Engine/Source}
 * @param excludedRoots directories of the root which are not indexed
 * @param moduleNames   modules the library is added to; they decide the defines the engine headers are indexed with
 */
public class UnrealEngineLibraryData extends AbstractExternalEntityData {
    private static final long serialVersionUID = 1L;

    /**
     * Processed after the modules and the build scripts module.
     */
    public static final Key<UnrealEngineLibraryData> KEY =
        Key.create(UnrealEngineLibraryData.class, ProjectKeys.CONTENT_ROOT.getProcessingWeight() + 4);

    private final String myLibraryName;
    private final String myRoot;
    private final List<String> myExcludedRoots;
    private final List<String> myModuleNames;

    public UnrealEngineLibraryData(String libraryName, String root, List<String> excludedRoots, List<String> moduleNames) {
        super(UnrealConstants.SYSTEM_ID);
        myLibraryName = libraryName;
        myRoot = root;
        myExcludedRoots = List.copyOf(excludedRoots);
        myModuleNames = List.copyOf(moduleNames);
    }

    public String getLibraryName() {
        return myLibraryName;
    }

    public String getRoot() {
        return myRoot;
    }

    public List<String> getExcludedRoots() {
        return myExcludedRoots;
    }

    public List<String> getModuleNames() {
        return myModuleNames;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof UnrealEngineLibraryData that) || !super.equals(o)) {
            return false;
        }
        return myLibraryName.equals(that.myLibraryName)
            && myRoot.equals(that.myRoot)
            && myExcludedRoots.equals(that.myExcludedRoots)
            && myModuleNames.equals(that.myModuleNames);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(myLibraryName, myRoot, myExcludedRoots, myModuleNames);
    }
}
