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
 * C# in the build scripts of an Unreal project: .NET and C# extensions of the {@code <Project>.Rules} module, bound to the
 * engine bundle, which provides the .NET reference assemblies and UnrealBuildTool. Loaded only with the C# plugin.
 */
@NullMarked
module consulo.unreal.csharp {
    requires consulo.unreal.base;
    requires consulo.csharp.base.impl;
    requires consulo.dotnet.impl;

    exports consulo.unreal.csharp;
}
