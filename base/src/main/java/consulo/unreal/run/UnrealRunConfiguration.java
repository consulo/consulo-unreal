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

import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfigurationModule;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.Module;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.nativeDev.debugger.run.NativeProgramRunConfiguration;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.unreal.UnrealConstants;
import consulo.unreal.UnrealUtil;
import consulo.unreal.engine.UnrealEngine;
import consulo.unreal.localize.UnrealLocalize;
import consulo.unreal.project.UProjectFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runs and debugs an Unreal project: the editor with it, the project as a game in the editor executable, or a built game
 * target. Debug goes through the native debugger (GDB or LLDB), with the engine's data formatters loaded.
 */
public class UnrealRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> implements NativeProgramRunConfiguration {
    /**
     * What is started: the executable, its arguments, where, and the engine it belongs to.
     */
    record Launch(Path executable, List<String> arguments, @Nullable Path workingDirectory, UnrealEngine engine) {
    }

    public UnrealRunConfiguration(String name, RunConfigurationModule configurationModule, ConfigurationFactory factory) {
        super(name, configurationModule, factory);
    }

    @Override
    protected Class<UnrealRunConfigurationOptions> getOptionsClass() {
        return UnrealRunConfigurationOptions.class;
    }

    @Override
    public UnrealRunConfigurationOptions getOptions() {
        return (UnrealRunConfigurationOptions) super.getOptions();
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(new UnrealRunFragment());
    }

    public UnrealRunMode getMode() {
        try {
            return UnrealRunMode.valueOf(getOptions().getMode());
        }
        catch (IllegalArgumentException e) {
            return UnrealRunMode.EDITOR;
        }
    }

    public void setMode(UnrealRunMode mode) {
        getOptions().setMode(mode.name());
    }

    /**
     * Modules imported from an Unreal project - each knows the {@code .uproject} it came from.
     */
    @Override
    public Collection<Module> getValidModules() {
        List<Module> modules = new ArrayList<>();
        for (Module module : getAllModules()) {
            if (ExternalSystemApiUtil.isExternalSystemAwareModule(UnrealConstants.SYSTEM_ID, module)) {
                modules.add(module);
            }
        }
        return modules;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationError {
        try {
            resolveLaunch();
        }
        catch (ExecutionException e) {
            throw new RuntimeConfigurationError(e.getMessage());
        }
    }

    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
        Launch launch = resolveLaunch();
        return new CommandLineState(environment) {
            @Override
            protected ProcessHandler startProcess() throws ExecutionException {
                return getProject().getApplication().getInstance(ProcessHandlerBuilderFactory.class)
                    .newBuilder(createCommandLine(launch))
                    .killable()
                    .colored()
                    .build();
            }
        };
    }

    @Override
    public NativeDebugTarget createDebugTarget(ExecutionEnvironment environment) throws ExecutionException {
        Launch launch = resolveLaunch();
        return NativeDebugTarget.launch(launch.executable(), launch.arguments(), launch.workingDirectory(), getEnvs(), null)
            .withSetup(new UnrealDebuggerSetup(launch.engine()))
            .withStepIntoWithoutSources(isStepIntoWithoutSources());
    }

    private GeneralCommandLine createCommandLine(Launch launch) {
        GeneralCommandLine commandLine = new GeneralCommandLine(launch.executable().toString());
        commandLine.addParameters(launch.arguments());
        if (launch.workingDirectory() != null) {
            commandLine.withWorkDirectory(launch.workingDirectory().toString());
        }
        commandLine.withEnvironment(getEnvs());
        commandLine.withParentEnvironmentType(isPassParentEnvs()
            ? GeneralCommandLine.ParentEnvironmentType.CONSOLE
            : GeneralCommandLine.ParentEnvironmentType.NONE);
        return commandLine;
    }

    Launch resolveLaunch() throws ExecutionException {
        Module module = getConfigurationModule().getModule();
        String linkedProjectPath = module == null ? null : ExternalSystemApiUtil.getExternalProjectPath(module);
        if (linkedProjectPath == null) {
            throw new ExecutionException(UnrealLocalize.runConfigurationNoProject().get());
        }

        UProjectFile projectFile;
        try {
            projectFile = UnrealUtil.readProjectFile(linkedProjectPath);
        }
        catch (ExternalSystemException | IllegalArgumentException e) {
            throw new ExecutionException(e.getMessage(), e);
        }

        Path engineHome = UnrealUtil.findEngineHome(UnrealUtil.findProjectSettings(getProject(), linkedProjectPath), linkedProjectPath);
        if (engineHome == null) {
            throw new ExecutionException(UnrealLocalize.errorNoEngine(projectFile.engineAssociation()).get());
        }
        UnrealEngine engine = new UnrealEngine(Platform.current(), engineHome);

        List<String> arguments = new ArrayList<>();
        Path executable;
        UnrealRunMode mode = getMode();
        if (mode == UnrealRunMode.STANDALONE) {
            String target = getOptions().getTarget();
            executable = engine.getTargetExecutable(projectFile.getDirectory(), target == null ? projectFile.name() : target, getOptions().getConfiguration());
        }
        else {
            executable = engine.getEditorExecutable();
            arguments.add(projectFile.file().toString());
            if (mode == UnrealRunMode.GAME) {
                arguments.add("-game");
            }
        }
        if (!Files.isRegularFile(executable)) {
            throw new ExecutionException(UnrealLocalize.runConfigurationNoExecutable(executable).get());
        }
        arguments.addAll(getProgramArguments());
        return new Launch(executable, List.copyOf(arguments), resolveWorkingDirectory(projectFile.getDirectory()), engine);
    }
}
