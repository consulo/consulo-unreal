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
package consulo.unreal.uasset.reader;

import java.util.Map;

/**
 * What the editor's asset registry knows about one asset of the package without loading it, e.g. for a Blueprint the
 * tags {@code ParentClass} and {@code NativeParentClass}.
 *
 * @param objectPath object path, without the package name for packages saved by recent engines
 * @param className  class of the asset; short ({@code Blueprint}) or full ({@code /Script/Engine.Blueprint})
 *
 * @author VISTALL
 * @since 2026-11-24
 */
public record UAssetRegistryObject(String objectPath, String className, Map<String, String> tags) {
}
