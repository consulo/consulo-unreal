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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The directories the import leaves out: build output of the project's plugins and, of the engine sources, everything a
 * game does not include.
 *
 * @author VISTALL
 * @since 2026-11-28
 */
public class UnrealUtilTest {
    @Test
    public void pluginDirectoriesAreTheOnesWithAPluginFile(@TempDir Path project) throws IOException {
        Path net = createFile(project.resolve("Plugins/MmoNet/MmoNet.uplugin"));
        Path nested = createFile(project.resolve("Plugins/Marketplace/Tools/Tools.uplugin"));
        createFile(project.resolve("Plugins/Marketplace/README.md"));
        // a copy of a plugin in build output is not a plugin of the project
        createFile(project.resolve("Plugins/MmoNet/Intermediate/Staged/MmoNet.uplugin"));

        List<Path> directories = UnrealUtil.findPluginDirectories(project);

        assertEquals(List.of(nested.getParent(), net.getParent()), directories);
    }

    @Test
    public void noPluginsDirectoryMeansNoPlugins(@TempDir Path project) {
        assertEquals(List.of(), UnrealUtil.findPluginDirectories(project));
    }

    @Test
    public void engineLibraryLeavesOutThirdPartyProgramsAndPrivateDirectories(@TempDir Path source) throws IOException {
        createFile(source.resolve("Runtime/Core/Public/CoreMinimal.h"));
        createFile(source.resolve("Runtime/Core/Private/Misc/Parse.cpp"));
        createFile(source.resolve("Runtime/Engine/Classes/GameFramework/Actor.h"));
        createFile(source.resolve("Runtime/Engine/Private/Actor.cpp"));
        createFile(source.resolve("Editor/UnrealEd/Internal/Editor.h"));
        createFile(source.resolve("Editor/UnrealEd/Private/Editor.cpp"));
        createFile(source.resolve("ThirdParty/zlib/zlib.h"));
        createFile(source.resolve("Programs/UnrealBuildTool/UnrealBuildTool.cs"));
        // a Private directory inside an excluded one is covered by that one
        createFile(source.resolve("ThirdParty/Foo/Private/foo.cpp"));

        List<String> excludes = UnrealUtil.findEngineLibraryExcludes(source);

        assertEquals(List.of(
            source.resolve("Editor/UnrealEd/Private").toString(),
            source.resolve("Programs").toString(),
            source.resolve("Runtime/Core/Private").toString(),
            source.resolve("Runtime/Engine/Private").toString(),
            source.resolve("ThirdParty").toString()
        ), excludes);
    }

    private static Path createFile(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "");
    }
}
