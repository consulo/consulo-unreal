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

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The header of a {@code .uasset} / {@code .umap} file: versions, the name, import and export tables and the asset
 * registry data. Object data (properties, bulk data) is not read.
 * <p>
 * Objects are referenced by package index: {@code 0} is null, {@code N > 0} is export {@code N - 1}, {@code N < 0} is
 * import {@code -N - 1}.
 *
 * @param fileVersionUE5         {@code 0} for packages saved before UE5
 * @param savedByEngineVersion   e.g. {@code 5.8.0-12345+++UE5+Release-5.8}, empty when the engine had no changelist
 * @param thumbnailTableOffset   {@code 0} when the package has no thumbnails
 *
 * @author VISTALL
 * @since 2026-11-24
 */
public record UAssetPackage(int legacyFileVersion,
                            int fileVersionUE4,
                            int fileVersionUE5,
                            int fileVersionLicensee,
                            int packageFlags,
                            String savedByEngineVersion,
                            int totalHeaderSize,
                            int thumbnailTableOffset,
                            long bulkDataStartOffset,
                            List<String> names,
                            List<UAssetImport> imports,
                            List<UAssetExport> exports,
                            List<UAssetRegistryObject> registryObjects) {
    public static final int PKG_CONTAINS_MAP = 0x00020000;
    public static final int PKG_UNVERSIONED_PROPERTIES = 0x00002000;
    public static final int PKG_FILTER_EDITOR_ONLY = 0x80000000;

    private static final String METADATA_CLASS = "/Script/CoreUObject.MetaData";

    public boolean isMap() {
        return (packageFlags & PKG_CONTAINS_MAP) != 0;
    }

    public @Nullable UAssetImport getImport(int packageIndex) {
        int index = -packageIndex - 1;
        return packageIndex < 0 && index < imports.size() ? imports.get(index) : null;
    }

    public @Nullable UAssetExport getExport(int packageIndex) {
        int index = packageIndex - 1;
        return packageIndex > 0 && index < exports.size() ? exports.get(index) : null;
    }

    public @Nullable String getObjectName(int packageIndex) {
        if (packageIndex < 0) {
            UAssetImport anImport = getImport(packageIndex);
            return anImport == null ? null : anImport.objectName();
        }
        UAssetExport export = getExport(packageIndex);
        return export == null ? null : export.objectName();
    }

    /**
     * Full path of the referenced object: {@code /Script/Engine.StaticMesh} for an import, {@code Cube} or
     * {@code BP_Foo.BP_Foo_C:Component} (relative to this package) for an export.
     */
    public @Nullable String getObjectPath(int packageIndex) {
        Deque<String> chain = new ArrayDeque<>();
        int current = packageIndex;
        // outers form a chain to a package; the bound protects against cycles in corrupt files
        for (int depth = 0; current != 0 && depth < 64; depth++) {
            if (current < 0) {
                UAssetImport anImport = getImport(current);
                if (anImport == null) {
                    return null;
                }
                chain.addFirst(anImport.objectName());
                current = anImport.outerIndex();
                if (current == 0 && !anImport.isPackage() && !UAssetImport.NONE.equals(anImport.packageName())) {
                    chain.addFirst(anImport.packageName());
                }
            }
            else {
                UAssetExport export = getExport(current);
                if (export == null) {
                    return null;
                }
                chain.addFirst(export.objectName());
                current = export.outerIndex();
            }
        }
        if (chain.isEmpty()) {
            return null;
        }

        StringBuilder path = new StringBuilder();
        int i = 0;
        for (String name : chain) {
            if (i == 1) {
                path.append('.');
            }
            else if (i > 1) {
                path.append(':');
            }
            path.append(name);
            i++;
        }
        return path.toString();
    }

    /**
     * The export the Content Browser shows for this file, or {@code null} for a package without assets. A Blueprint
     * package has two asset exports, the {@code Blueprint} object and its generated class {@code BP_Foo_C}; the one named
     * like the file wins.
     *
     * @param assetName file name without extension
     */
    public @Nullable UAssetExport getMainAsset(String assetName) {
        UAssetExport firstAsset = null;
        for (UAssetExport export : exports) {
            if (export.isAsset()) {
                if (export.outerIndex() == 0 && export.objectName().equals(assetName)) {
                    return export;
                }
                if (firstAsset == null) {
                    firstAsset = export;
                }
            }
        }
        if (firstAsset != null) {
            return firstAsset;
        }
        // packages saved before exports were marked as assets: the first top-level object besides the package metadata
        for (UAssetExport export : exports) {
            if (export.outerIndex() == 0 && !METADATA_CLASS.equals(getObjectPath(export.classIndex()))) {
                return export;
            }
        }
        return null;
    }

    /**
     * Class of the {@link #getMainAsset(String) main asset}, e.g. {@code /Script/Engine.StaticMesh}.
     */
    public @Nullable String getMainAssetClass(String assetName) {
        UAssetExport mainAsset = getMainAsset(assetName);
        return mainAsset == null ? null : getObjectPath(mainAsset.classIndex());
    }

    public @Nullable String getRegistryTag(String tag) {
        for (UAssetRegistryObject object : registryObjects) {
            String value = object.tags().get(tag);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
