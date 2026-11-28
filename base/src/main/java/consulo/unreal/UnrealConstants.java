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

import consulo.externalSystem.model.ProjectSystemId;
import consulo.unreal.icon.UnrealIconGroup;
import consulo.unreal.localize.UnrealLocalize;

import java.util.List;

public interface UnrealConstants {
    ProjectSystemId SYSTEM_ID = new ProjectSystemId("UNREAL", UnrealLocalize.systemName(), UnrealIconGroup.unreal());

    String PROJECT_FILE_EXTENSION = ".uproject";
    String PLUGIN_FILE_EXTENSION = ".uplugin";
    String BUILD_RULES_SUFFIX = ".Build.cs";
    String TARGET_RULES_SUFFIX = ".Target.cs";
    String EDITOR_TARGET_SUFFIX = "Editor";

    String SOURCE_DIRECTORY = "Source";
    String PLUGINS_DIRECTORY = "Plugins";
    String INTERMEDIATE_DIRECTORY = "Intermediate";

    /**
     * Directories of a project that are build output or caches.
     */
    List<String> EXCLUDED_DIRECTORIES = List.of("Binaries", INTERMEDIATE_DIRECTORY, "Saved", "DerivedDataCache", ".vs");

    /**
     * Directories of the project directory only: {@code Build} holds platform packaging files, not code.
     */
    List<String> EXCLUDED_PROJECT_DIRECTORIES = List.of("Build");

    /**
     * Build output of a plugin, next to its {@code .uplugin} file.
     */
    List<String> EXCLUDED_PLUGIN_DIRECTORIES = List.of("Binaries", INTERMEDIATE_DIRECTORY, "Saved");

    /**
     * Directories of {@code Engine/Source} that are not part of the engine modules a game compiles against.
     */
    List<String> EXCLUDED_ENGINE_SOURCE_DIRECTORIES = List.of("ThirdParty");

    /**
     * The implementation of an engine module: a game only includes the other directories ({@code Public},
     * {@code Classes}, {@code Internal}), so the engine library leaves it out - about half of the engine sources.
     */
    String ENGINE_PRIVATE_DIRECTORY = "Private";

    /**
     * Suffix of the library of the engine C++ sources, after the engine module name: {@code UE 5.8.4 Sources}.
     */
    String ENGINE_LIBRARY_SUFFIX = " Sources";

    String ENGINE_PROGRAMS_DIRECTORY = "Programs";

    /**
     * The only {@code Engine/Source/Programs} kept: UnrealBuildTool and the {@code EpicGames.*} libraries it is built
     * from, which the build scripts compile against.
     */
    List<String> KEPT_ENGINE_PROGRAMS = List.of("UnrealBuildTool", "Shared");

    /**
     * Suffix of the module holding the build scripts of a project.
     */
    String RULES_MODULE_SUFFIX = ".Rules";

    /**
     * Assemblies the build scripts are compiled against - what UnrealBuildTool references in
     * {@code DynamicCompilation.CompileAssembly} ({@code typeof(object)}, {@code typeof(UnrealBuildTool)},
     * {@code typeof(FileReference)}, {@code typeof(UEBuildPlatformSDK)} and the assembly names it loads), plus
     * {@code mscorlib}. Looked up in the .NET runtime the engine bundles, then next to {@code UnrealBuildTool.dll}.
     */
    List<String> RULES_REFERENCES = List.of(
        "System.Private.CoreLib",
        "mscorlib",
        "netstandard",
        "System.Runtime",
        "System.Runtime.Extensions",
        "System.CodeDom",
        "System.Collections",
        "System.Collections.Concurrent",
        "System.Console",
        "System.IO",
        "System.IO.FileSystem",
        "System.Linq",
        "System.Private.Uri",
        "System.Private.Xml",
        "System.Private.Xml.Linq",
        "System.Text.Json",
        "System.Text.RegularExpressions",
        "System.Threading.Tasks",
        "System.Threading.Tasks.Parallel",
        "Microsoft.CodeAnalysis.CSharp",
        "Microsoft.Extensions.Logging.Abstractions",
        "UnrealBuildTool",
        "EpicGames.Build",
        "EpicGames.Core"
    );

    /**
     * Module extensions of the build scripts, parents first. They live in the optional C# part of the plugin, so they are
     * referenced by id.
     */
    String RULES_DOTNET_EXTENSION_ID = "unreal-dotnet";
    String RULES_CSHARP_EXTENSION_ID = "unreal-csharp";
    List<String> RULES_EXTENSION_IDS = List.of(RULES_DOTNET_EXTENSION_ID, RULES_CSHARP_EXTENSION_ID);

    String DEFAULT_CONFIGURATION = "Development";
    List<String> CONFIGURATIONS = List.of("Debug", "DebugGame", DEFAULT_CONFIGURATION, "Test", "Shipping");

    String TASK_BUILD = "build";
    String TASK_REBUILD = "rebuild";
    String TASK_CLEAN = "clean";
    String TASK_EDITOR = "editor";
    String TASK_GAME = "game";
}
