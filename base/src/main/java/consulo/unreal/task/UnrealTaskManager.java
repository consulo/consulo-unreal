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

package consulo.unreal.task;

import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import consulo.process.cmd.GeneralCommandLine;
import consulo.unreal.UnrealConstants;
import consulo.unreal.UnrealUtil;
import consulo.unreal.engine.UnrealEngine;
import consulo.unreal.localize.UnrealLocalize;
import consulo.unreal.project.UProjectFile;
import consulo.unreal.setting.UnrealExecutionSettings;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the tasks the resolver offers: {@code build}, {@code rebuild} and {@code clean} with UnrealBuildTool, {@code editor}
 * and {@code game} with the Unreal Editor. Script parameters of a task run configuration are appended to the command.
 */
public class UnrealTaskManager implements ExternalSystemTaskManager<UnrealExecutionSettings> {
    /**
     * The engine's own editor target, built for a Blueprint-only project.
     */
    private static final String ENGINE_EDITOR_TARGET = "UnrealEditor";

    private final UnrealCommand myCommand = new UnrealCommand();

    @Override
    public void executeTasks(ExternalSystemTaskId id,
                             List<String> taskNames,
                             String projectPath,
                             @Nullable UnrealExecutionSettings settings,
                             List<String> vmOptions,
                             List<String> scriptParameters,
                             @Nullable String debuggerSetup,
                             ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
        UProjectFile projectFile = UnrealUtil.readProjectFile(projectPath);
        UnrealEngine engine = UnrealUtil.toEngine(settings, projectFile);
        String target = UnrealUtil.findTarget(projectFile, settings == null ? null : settings.getTarget());
        String buildTarget = target == null ? ENGINE_EDITOR_TARGET : target;
        String configuration = settings == null ? UnrealConstants.DEFAULT_CONFIGURATION : settings.getConfiguration();

        for (String taskName : taskNames) {
            switch (taskName) {
                case UnrealConstants.TASK_BUILD -> build(id, engine, projectFile, buildTarget, configuration, scriptParameters, listener);
                case UnrealConstants.TASK_CLEAN -> clean(id, engine, projectFile, buildTarget, configuration, listener);
                case UnrealConstants.TASK_REBUILD -> {
                    clean(id, engine, projectFile, buildTarget, configuration, listener);
                    build(id, engine, projectFile, buildTarget, configuration, scriptParameters, listener);
                }
                case UnrealConstants.TASK_EDITOR -> runEditor(id, engine, projectFile, scriptParameters, listener);
                case UnrealConstants.TASK_GAME -> {
                    List<String> arguments = new ArrayList<>(List.of("-game", "-windowed"));
                    arguments.addAll(scriptParameters);
                    runEditor(id, engine, projectFile, arguments, listener);
                }
                default -> throw new ExternalSystemException(UnrealLocalize.errorUnknownTask(taskName).get());
            }
        }
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId id, ExternalSystemTaskNotificationListener listener) {
        return myCommand.cancel();
    }

    private void build(ExternalSystemTaskId id,
                       UnrealEngine engine,
                       UProjectFile projectFile,
                       String target,
                       String configuration,
                       List<String> extraArguments,
                       ExternalSystemTaskNotificationListener listener) {
        GeneralCommandLine commandLine = engine.createBuildCommandLine(target, configuration, projectFile.file(), extraArguments);
        myCommand.run(id, checkBuildScript(engine, commandLine), "UnrealBuildTool " + target, listener);
    }

    private void clean(ExternalSystemTaskId id,
                       UnrealEngine engine,
                       UProjectFile projectFile,
                       String target,
                       String configuration,
                       ExternalSystemTaskNotificationListener listener) {
        GeneralCommandLine commandLine = engine.createBuildCommandLine(target, configuration, projectFile.file(), List.of("-Clean"));
        myCommand.run(id, checkBuildScript(engine, commandLine), "UnrealBuildTool -Clean " + target, listener);
    }

    private void runEditor(ExternalSystemTaskId id,
                           UnrealEngine engine,
                           UProjectFile projectFile,
                           List<String> arguments,
                           ExternalSystemTaskNotificationListener listener) {
        if (!engine.getEditorExecutable().toFile().canExecute()) {
            throw new ExternalSystemException(UnrealLocalize.errorNoEditor(engine.getEditorExecutable()).get());
        }
        myCommand.run(id, engine.createEditorCommandLine(projectFile.file(), arguments), "UnrealEditor", listener);
    }

    private static GeneralCommandLine checkBuildScript(UnrealEngine engine, GeneralCommandLine commandLine) {
        if (!engine.getBuildScript().toFile().isFile()) {
            throw new ExternalSystemException(UnrealLocalize.errorNoBuildScript(engine.getBuildScript()).get());
        }
        return commandLine;
    }
}
