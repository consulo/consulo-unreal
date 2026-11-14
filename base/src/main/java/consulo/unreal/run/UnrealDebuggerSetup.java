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

import consulo.application.Application;
import consulo.nativeDev.debugger.NativeDebuggerInstallation;
import consulo.nativeDev.debugger.NativeDebuggerProvider;
import consulo.nativeDev.debugger.driver.NativeDebuggerSetup;
import consulo.platform.Platform;
import consulo.unreal.engine.UnrealEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads the data formatters the engine ships for its types ({@code FString}, {@code FName}, {@code TArray}, {@code TMap},
 * ...) into the debugger: {@code Engine/Extras/GDBPrinters} for GDB, {@code Engine/Extras/LLDBDataFormatters} for LLDB.
 * <p>
 * The engine has about 5 GB of DWARF in its {@code .debug} files. GDB indexes all of it once the libraries load and grows past
 * 30 GB; LLDB with symbols loaded on demand stays under 8 GB, so "Auto" prefers LLDB for Unreal targets.
 */
public class UnrealDebuggerSetup implements NativeDebuggerSetup {
    private final Path myEngineHome;

    public UnrealDebuggerSetup(UnrealEngine engine) {
        myEngineHome = engine.getHome();
    }

    @Override
    public List<String> getSourceLanguages() {
        return List.of("cpp");
    }

    @Override
    public List<String> getInitCommands(NativeDebuggerProvider debugger) {
        Path extras = myEngineHome.resolve("Engine").resolve("Extras");
        if (NativeDebuggerProvider.GDB_FAMILY.equals(debugger.getFamilyId())) {
            Path printers = extras.resolve("GDBPrinters");
            if (!Files.isRegularFile(printers.resolve("UEPrinters.py"))) {
                return List.of();
            }
            // UEPrinters.py uses gdb.printing without importing it
            return List.of("python import sys; import gdb.printing; sys.path.insert(0, '" + escape(printers) + "'); "
                + "from UEPrinters import register_ue_printers; register_ue_printers(None)");
        }
        if (NativeDebuggerProvider.LLDB_FAMILY.equals(debugger.getFamilyId())) {
            // TCHAR is char16_t on every platform but Windows, where it is wchar_t - also 2 bytes
            Path formatters = extras.resolve("LLDBDataFormatters").resolve("UEDataFormatters_2ByteChars.py");
            List<String> commands = new ArrayList<>();
            commands.add("settings set symbols.load-on-demand true");
            if (Files.isRegularFile(formatters)) {
                commands.add("command script import \"" + formatters + "\"");
            }
            return commands;
        }
        return List.of();
    }

    @Override
    public List<NativeDebuggerInstallation> getToolchainDebuggers() {
        List<NativeDebuggerProvider> providers = new ArrayList<>();
        Application.get().getExtensionPoint(NativeDebuggerProvider.class).forEach(provider -> {
            if (NativeDebuggerProvider.LLDB_FAMILY.equals(provider.getFamilyId())) {
                providers.add(provider);
            }
        });
        Platform platform = Platform.current();
        providers.sort(Comparator.comparingInt((NativeDebuggerProvider provider) -> provider.getPriority(platform)).reversed());

        List<NativeDebuggerInstallation> installations = new ArrayList<>();
        for (NativeDebuggerProvider provider : providers) {
            installations.addAll(provider.findInstallations(platform));
        }
        return installations;
    }

    private static String escape(Path path) {
        return path.toString().replace("\\", "\\\\").replace("'", "\\'");
    }
}
