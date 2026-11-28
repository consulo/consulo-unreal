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

/**
 * An object of another package this one references, e.g. class {@code /Script/Engine.StaticMesh}: {@code ObjectName}
 * {@code StaticMesh} with class {@code /Script/CoreUObject.Class} and outer the import of package {@code /Script/Engine}.
 *
 * @param outerIndex  package index (see {@link UAssetPackage}) of the outer object, {@code 0} for a package
 * @param packageName package of an object whose outer chain does not end at a package (actors saved in their own
 *                    package by World Partition), otherwise {@code None}
 *
 * @author VISTALL
 * @since 2026-11-24
 */
public record UAssetImport(String classPackage, String className, int outerIndex, String objectName, String packageName) {
    public static final String NONE = "None";

    public boolean isPackage() {
        return outerIndex == 0 && "Package".equals(className);
    }
}
