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

import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.service.project.AbstractExternalEntityData;
import consulo.unreal.UnrealConstants;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Run configurations to create for an imported project - a child of its {@link ProjectKeys#PROJECT} node.
 *
 * @param projectModuleName the module of the project file the configurations run ({@code <Project>.uproject})
 */
public class UnrealRunData extends AbstractExternalEntityData {
    private static final long serialVersionUID = 1L;

    /**
     * Processed after the modules exist.
     */
    public static final Key<UnrealRunData> KEY = Key.create(UnrealRunData.class, ProjectKeys.CONTENT_ROOT.getProcessingWeight() + 3);

    private final String myProjectModuleName;
    private final String myProjectName;

    public UnrealRunData(String projectModuleName, String projectName) {
        super(UnrealConstants.SYSTEM_ID);
        myProjectModuleName = projectModuleName;
        myProjectName = projectName;
    }

    public String getProjectModuleName() {
        return myProjectModuleName;
    }

    public String getProjectName() {
        return myProjectName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof UnrealRunData that) || !super.equals(o)) {
            return false;
        }
        return myProjectModuleName.equals(that.myProjectModuleName) && myProjectName.equals(that.myProjectName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(myProjectModuleName, myProjectName);
    }
}
