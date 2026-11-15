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

package consulo.unreal.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import consulo.content.bundle.Sdk;
import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;
import consulo.process.cmd.GeneralCommandLine;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * An engine installation - an installed build from the launcher, an unpacked binary build or a source build. Its home is
 * the directory holding {@code Engine/}.
 */
public final class UnrealEngine {
    public static final String BUILD_VERSION_FILE = "Engine/Build/Build.version";

    private final Platform myPlatform;
    private final Path myHome;

    public UnrealEngine(Platform platform, Path home) {
        myPlatform = platform;
        myHome = home;
    }

    public static boolean isValidHome(Path home) {
        return Files.isRegularFile(home.resolve(BUILD_VERSION_FILE));
    }

    public static @Nullable UnrealEngine fromSdk(@Nullable Sdk sdk) {
        String homePath = sdk == null ? null : sdk.getHomePath();
        if (homePath == null) {
            return null;
        }
        try {
            Path home = Path.of(homePath);
            return isValidHome(home) ? new UnrealEngine(Platform.current(), home) : null;
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    public Path getHome() {
        return myHome;
    }

    public Path getSourceDirectory() {
        return myHome.resolve("Engine").resolve("Source");
    }

    /**
     * The directory of {@code UnrealBuildTool.dll} and the {@code EpicGames.*} assemblies it is built from.
     */
    public Path getBuildToolDirectory() {
        return myHome.resolve("Engine").resolve("Binaries").resolve("DotNET").resolve("UnrealBuildTool");
    }

    /**
     * Assemblies of the .NET the engine bundles for UnrealBuildTool, the newest one for the host: the runtime
     * ({@code Engine/Binaries/ThirdParty/DotNet/<version>/<platform>/shared/Microsoft.NETCore.App/<version>}) where the
     * types are implemented ({@code System.Private.CoreLib} holds {@code Object}, {@code List<T>}, ...), else the
     * reference pack ({@code packs/Microsoft.NETCore.App.Ref/<version>/ref/net<version>}). The reference assemblies of
     * {@code System.Runtime}, {@code System.Collections}, ... carry metadata only and forward types, which the .NET
     * support does not follow, so the runtime is preferred.
     */
    public @Nullable Path findDotNetAssemblies() {
        Path dotNet = myHome.resolve("Engine").resolve("Binaries").resolve("ThirdParty").resolve("DotNet");
        String hostPrefix = myPlatform.os().isWindows() ? "win-" : myPlatform.os().isMac() ? "osx-" : "linux-";
        String arch = CpuArchitecture.AARCH64.equals(myPlatform.os().arch()) ? "arm64" : "x64";
        String host = hostPrefix + arch;
        List<Path> directories;
        try (Stream<Path> paths = Files.walk(dotNet, 7)) {
            directories = paths.filter(Files::isDirectory).filter(path -> path.toString().contains(host)).toList();
        }
        catch (IOException e) {
            return null;
        }
        Path runtime = directories.stream()
            .filter(path -> Files.isRegularFile(path.resolve("System.Private.CoreLib.dll")))
            .max(Comparator.comparing(Path::toString))
            .orElse(null);
        if (runtime != null) {
            return runtime;
        }
        return directories.stream()
            .filter(path -> path.toString().contains("Microsoft.NETCore.App.Ref"))
            .filter(path -> path.getParent() != null && path.getParent().getFileName().toString().equals("ref"))
            .max(Comparator.comparing(Path::toString))
            .orElse(null);
    }

    /**
     * @return {@code major.minor.patch} from {@code Engine/Build/Build.version}
     */
    public @Nullable String readVersion() {
        try {
            JsonElement root = JsonParser.parseString(Files.readString(myHome.resolve(BUILD_VERSION_FILE)));
            if (!(root instanceof JsonObject version)) {
                return null;
            }
            return number(version, "MajorVersion") + "." + number(version, "MinorVersion") + "." + number(version, "PatchVersion");
        }
        catch (IOException | JsonParseException | IllegalStateException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * The platform name UnrealBuildTool uses for the host.
     */
    public String getHostPlatformName() {
        if (myPlatform.os().isWindows()) {
            return "Win64";
        }
        if (myPlatform.os().isMac()) {
            return "Mac";
        }
        return "Linux";
    }

    /**
     * {@code Build.sh}/{@code Build.bat}: sets up the bundled .NET SDK and runs UnrealBuildTool with the given arguments.
     */
    public Path getBuildScript() {
        Path batchFiles = myHome.resolve("Engine").resolve("Build").resolve("BatchFiles");
        if (myPlatform.os().isWindows()) {
            return batchFiles.resolve("Build.bat");
        }
        return batchFiles.resolve(myPlatform.os().isMac() ? "Mac" : "Linux").resolve("Build.sh");
    }

    public Path getEditorExecutable() {
        Path binaries = myHome.resolve("Engine").resolve("Binaries");
        if (myPlatform.os().isWindows()) {
            return binaries.resolve("Win64").resolve("UnrealEditor.exe");
        }
        if (myPlatform.os().isMac()) {
            return binaries.resolve("Mac").resolve("UnrealEditor.app/Contents/MacOS/UnrealEditor");
        }
        return binaries.resolve("Linux").resolve("UnrealEditor");
    }

    /**
     * The executable UnrealBuildTool writes for a program or game target: {@code Binaries/<Platform>/<Target>}, with
     * {@code -<Platform>-<Configuration>} for configurations other than Development.
     */
    public Path getTargetExecutable(Path projectDirectory, String target, String configuration) {
        String name = "Development".equals(configuration) ? target : target + "-" + getHostPlatformName() + "-" + configuration;
        if (myPlatform.os().isWindows()) {
            name += ".exe";
        }
        else if (myPlatform.os().isMac()) {
            name += ".app/Contents/MacOS/" + target;
        }
        return projectDirectory.resolve("Binaries").resolve(getHostPlatformName()).resolve(name);
    }

    /**
     * {@code <build script> <target> <platform> <configuration> -Project=<project file> <extra>}
     */
    public GeneralCommandLine createBuildCommandLine(String target, String configuration, Path projectFile, List<String> extraArguments) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withExecutablePath(getBuildScript());
        commandLine.withWorkingDirectory(myHome);
        commandLine.withPlatform(myPlatform);
        commandLine.addParameters(target, getHostPlatformName(), configuration, "-Project=" + projectFile);
        commandLine.addParameters(extraArguments);
        return commandLine;
    }

    public GeneralCommandLine createEditorCommandLine(Path projectFile, List<String> extraArguments) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withExecutablePath(getEditorExecutable());
        commandLine.withWorkingDirectory(myHome);
        commandLine.withPlatform(myPlatform);
        commandLine.addParameter(projectFile.toString());
        commandLine.addParameters(extraArguments);
        return commandLine;
    }

    private static int number(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null ? 0 : element.getAsInt();
    }
}
