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

import org.jspecify.annotations.NullMarked;

/**
 * Unreal Engine projects as an external system: importing {@code .uproject} files with UnrealBuildTool, the engine as a
 * bundle, C++ include paths and defines of every module, auto-reload and build tasks.
 */
@NullMarked
module consulo.unreal.base {
    requires transitive consulo.external.system.api;
    requires transitive consulo.application.content.api;
    requires transitive consulo.process.api;
    requires consulo.cpp.api;
    requires consulo.nativeDev.debugger.api;
    requires consulo.module.creation.api;
    requires consulo.module.ui.api;
    requires consulo.project.ui.api;
    requires com.google.gson;
    requires org.jdom;

    exports consulo.unreal;
    exports consulo.unreal.engine;
    exports consulo.unreal.execution;
    exports consulo.unreal.icon;
    exports consulo.unreal.importing;
    exports consulo.unreal.localize;
    exports consulo.unreal.module;
    exports consulo.unreal.project;
    exports consulo.unreal.run;
    exports consulo.unreal.setting;
    exports consulo.unreal.task;
    exports consulo.unreal.ui;

    opens consulo.unreal.setting to consulo.util.xml.serializer;
    opens consulo.unreal.run to consulo.util.xml.serializer, consulo.execution.api;
}
