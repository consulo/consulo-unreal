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

package consulo.unreal;

import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.project.Project;
import consulo.unreal.setting.UnrealProjectSettings;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A project is out of date when its {@code .uproject}, a {@code .uplugin} or the rules of a module or target change.
 */
public class UnrealAutoImportAware implements ExternalSystemAutoImportAware {
    @Override
    public @Nullable String getAffectedExternalProjectPath(String changedFileOrDirPath, Project project) {
        Path changed = Path.of(changedFileOrDirPath);
        if (!UnrealUtil.isBuildFile(changed)) {
            return null;
        }
        UnrealProjectSettings settings = UnrealUtil.findProjectSettings(project, changedFileOrDirPath);
        if (settings != null) {
            return settings.getExternalProjectPath();
        }
        for (Path directory = changed.getParent(); directory != null; directory = directory.getParent()) {
            UnrealProjectSettings enclosing = UnrealUtil.findProjectSettings(project, directory.toString());
            if (enclosing != null) {
                return enclosing.getExternalProjectPath();
            }
        }
        return null;
    }

    @Override
    public List<Path> getAffectedExternalProjectFilePaths(String projectPath, Project project) {
        List<Path> files = new ArrayList<>();
        Path projectFile = UnrealUtil.findProjectFile(Path.of(projectPath));
        if (projectFile != null) {
            files.add(projectFile);
        }
        files.addAll(UnrealUtil.findBuildFiles(UnrealUtil.toProjectDirectory(Path.of(projectPath))));
        return files;
    }

}
