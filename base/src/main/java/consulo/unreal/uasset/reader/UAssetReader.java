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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the header of editor (uncooked) packages saved by UE 4.x and UE5, following {@code FPackageFileSummary},
 * {@code FObjectImport} and {@code FObjectExport} serialization of the engine.
 * <p>
 * Cooked and unversioned packages are rejected: their layout depends on the engine that cooked them, and their exports
 * live in {@code .uexp} files.
 *
 * @author VISTALL
 * @since 2026-11-24
 */
public final class UAssetReader {
    public static final int PACKAGE_FILE_TAG = 0x9E2A83C1;

    /**
     * Newest summary layout this reader knows; {@code -9} promises that layout changes only follow new object versions.
     */
    private static final int CURRENT_LEGACY_FILE_VERSION = -9;

    // EUnrealEngineObjectUE4Version
    private static final int VER_UE4_OLDEST_LOADABLE_PACKAGE = 214;
    private static final int VER_UE4_WORLD_LEVEL_INFO = 224;
    private static final int VER_UE4_CHANGED_CHUNKID_TO_BE_AN_ARRAY_OF_CHUNKIDS = 326;
    private static final int VER_UE4_ENGINE_VERSION_OBJECT = 336;
    private static final int VER_UE4_LOAD_FOR_EDITOR_GAME = 365;
    private static final int VER_UE4_ADD_STRING_ASSET_REFERENCES_MAP = 384;
    private static final int VER_UE4_PACKAGE_SUMMARY_HAS_COMPATIBLE_ENGINE_VERSION = 444;
    private static final int VER_UE4_SERIALIZE_TEXT_IN_PACKAGES = 459;
    private static final int VER_UE4_COOKED_ASSETS_IN_EDITOR_SUPPORT = 485;
    private static final int VER_UE4_NAME_HASHES_SERIALIZED = 504;
    private static final int VER_UE4_PRELOAD_DEPENDENCIES_IN_COOKED_EXPORTS = 507;
    private static final int VER_UE4_TEMPLATE_INDEX_IN_COOKED_EXPORTS = 508;
    private static final int VER_UE4_ADDED_SEARCHABLE_NAMES = 510;
    private static final int VER_UE4_64BIT_EXPORTMAP_SERIALSIZES = 511;
    private static final int VER_UE4_ADDED_PACKAGE_SUMMARY_LOCALIZATION_ID = 516;
    private static final int VER_UE4_ADDED_PACKAGE_OWNER = 518;
    private static final int VER_UE4_NON_OUTER_PACKAGE_IMPORT = 520;
    private static final int VER_UE4_ASSETREGISTRY_DEPENDENCYFLAGS = 521;
    private static final int VER_UE4_AUTOMATIC_VERSION = 522;

    // EUnrealEngineObjectUE5Version
    private static final int VER_UE5_NAMES_REFERENCED_FROM_EXPORT_DATA = 1001;
    private static final int VER_UE5_PAYLOAD_TOC = 1002;
    private static final int VER_UE5_OPTIONAL_RESOURCES = 1003;
    private static final int VER_UE5_REMOVE_OBJECT_EXPORT_PACKAGE_GUID = 1005;
    private static final int VER_UE5_TRACK_OBJECT_EXPORT_IS_INHERITED = 1006;
    private static final int VER_UE5_ADD_SOFTOBJECTPATH_LIST = 1008;
    private static final int VER_UE5_DATA_RESOURCES = 1009;
    private static final int VER_UE5_SCRIPT_SERIALIZATION_OFFSET = 1010;
    private static final int VER_UE5_METADATA_SERIALIZATION_OFFSET = 1014;
    private static final int VER_UE5_VERSE_CELLS = 1015;
    private static final int VER_UE5_PACKAGE_SAVED_HASH = 1016;
    private static final int VER_UE5_IMPORT_TYPE_HIERARCHIES = 1018;
    private static final int VER_UE5_AUTOMATIC_VERSION = 1018;

    private static final int GUID_SIZE = 16;
    private static final int IO_HASH_SIZE = 20;

    private UAssetReader() {
    }

    public static boolean hasPackageTag(ByteBuffer buffer) {
        return buffer.remaining() >= 4 && Integer.reverseBytes(buffer.getInt(buffer.position())) == PACKAGE_FILE_TAG;
    }

    public static UAssetPackage read(ByteBuffer buffer) throws UAssetReadException {
        UAssetArchive archive = new UAssetArchive(buffer);

        if (archive.size() < 32 || archive.readInt32() != PACKAGE_FILE_TAG) {
            throw new UAssetReadException("Not an Unreal package");
        }

        int legacyFileVersion = archive.readInt32();
        if (legacyFileVersion >= 0) {
            throw new UAssetReadException("Unreal Engine 3 packages are not supported");
        }
        if (legacyFileVersion < CURRENT_LEGACY_FILE_VERSION) {
            throw new UAssetReadException("Package summary version " + legacyFileVersion + " is newer than supported");
        }
        if (legacyFileVersion != -4) {
            // UE3 version
            archive.readInt32();
        }
        int ue4 = archive.readInt32();
        int ue5 = legacyFileVersion <= -8 ? archive.readInt32() : 0;
        int licensee = archive.readInt32();

        if (ue4 == 0 && ue5 == 0 && licensee == 0) {
            throw new UAssetReadException("Unversioned (cooked) packages are not supported");
        }
        if (ue4 < VER_UE4_OLDEST_LOADABLE_PACKAGE) {
            throw new UAssetReadException("Package version " + ue4 + " is too old");
        }
        if (ue4 > VER_UE4_AUTOMATIC_VERSION || ue5 > VER_UE5_AUTOMATIC_VERSION) {
            throw new UAssetReadException("Package version " + ue4 + "/" + ue5 + " is newer than supported");
        }

        int totalHeaderSize = 0;
        if (ue5 >= VER_UE5_PACKAGE_SAVED_HASH) {
            archive.skip(IO_HASH_SIZE);
            totalHeaderSize = archive.readInt32();
        }
        if (legacyFileVersion <= -2) {
            skipCustomVersions(archive, legacyFileVersion);
        }
        if (ue5 < VER_UE5_PACKAGE_SAVED_HASH) {
            totalHeaderSize = archive.readInt32();
        }

        // package name, unused
        archive.readFString();
        int packageFlags = archive.readInt32();
        boolean filterEditorOnly = (packageFlags & UAssetPackage.PKG_FILTER_EDITOR_ONLY) != 0;
        if (filterEditorOnly) {
            throw new UAssetReadException("Cooked packages are not supported");
        }

        int nameCount = archive.readInt32();
        int nameOffset = archive.readInt32();
        if (ue5 >= VER_UE5_ADD_SOFTOBJECTPATH_LIST) {
            // soft object paths count, offset
            archive.skip(8);
        }
        if (ue4 >= VER_UE4_ADDED_PACKAGE_SUMMARY_LOCALIZATION_ID) {
            // localization id
            archive.readFString();
        }
        if (ue4 >= VER_UE4_SERIALIZE_TEXT_IN_PACKAGES) {
            // gatherable text data count, offset
            archive.skip(8);
        }
        int exportCount = archive.readInt32();
        int exportOffset = archive.readInt32();
        int importCount = archive.readInt32();
        int importOffset = archive.readInt32();
        if (ue5 >= VER_UE5_VERSE_CELLS) {
            // cell export count, offset, cell import count, offset
            archive.skip(16);
        }
        if (ue5 >= VER_UE5_METADATA_SERIALIZATION_OFFSET) {
            // metadata offset
            archive.skip(4);
        }
        // depends offset
        archive.skip(4);
        if (ue4 >= VER_UE4_ADD_STRING_ASSET_REFERENCES_MAP) {
            // soft package references count, offset
            archive.skip(8);
        }
        if (ue4 >= VER_UE4_ADDED_SEARCHABLE_NAMES) {
            // searchable names offset
            archive.skip(4);
        }
        int thumbnailTableOffset = archive.readInt32();
        if (ue5 >= VER_UE5_IMPORT_TYPE_HIERARCHIES) {
            // import type hierarchies count, offset
            archive.skip(8);
        }
        if (ue5 < VER_UE5_PACKAGE_SAVED_HASH) {
            // package guid
            archive.skip(GUID_SIZE);
        }
        if (ue4 >= VER_UE4_ADDED_PACKAGE_OWNER) {
            // persistent guid
            archive.skip(GUID_SIZE);
            if (ue4 < VER_UE4_NON_OUTER_PACKAGE_IMPORT) {
                // owner persistent guid
                archive.skip(GUID_SIZE);
            }
        }

        int generationCount = archive.readCount(8);
        // export count, name count per generation
        archive.skip(generationCount * 8);

        String savedByEngineVersion = "";
        if (ue4 >= VER_UE4_ENGINE_VERSION_OBJECT) {
            savedByEngineVersion = readEngineVersion(archive);
        }
        else {
            // engine changelist
            archive.skip(4);
        }
        if (ue4 >= VER_UE4_PACKAGE_SUMMARY_HAS_COMPATIBLE_ENGINE_VERSION) {
            readEngineVersion(archive);
        }

        // compression flags
        archive.skip(4);
        if (archive.readInt32() != 0) {
            throw new UAssetReadException("Packages with package level compression are not supported");
        }
        // package source
        archive.skip(4);
        int additionalPackagesToCook = archive.readCount(4);
        for (int i = 0; i < additionalPackagesToCook; i++) {
            archive.readFString();
        }
        if (legacyFileVersion > -7) {
            // texture allocations
            archive.skip(4);
        }
        int assetRegistryDataOffset = archive.readInt32();
        long bulkDataStartOffset = archive.readInt64();
        if (ue4 >= VER_UE4_WORLD_LEVEL_INFO) {
            // world tile info data offset
            archive.skip(4);
        }
        if (ue4 >= VER_UE4_CHANGED_CHUNKID_TO_BE_AN_ARRAY_OF_CHUNKIDS) {
            int chunkCount = archive.readCount(4);
            archive.skip(chunkCount * 4);
        }
        // the rest of the summary (preload dependencies, payload toc, data resources) is not needed

        List<String> names = readNames(archive, ue4, nameCount, nameOffset);
        archive.setNames(names);
        List<UAssetImport> imports = readImports(archive, ue4, ue5, importCount, importOffset);
        List<UAssetExport> exports = readExports(archive, ue4, ue5, packageFlags, exportCount, exportOffset);
        List<UAssetRegistryObject> registryObjects = readRegistryObjects(archive, ue4, assetRegistryDataOffset);

        return new UAssetPackage(legacyFileVersion,
            ue4,
            ue5,
            licensee,
            packageFlags,
            savedByEngineVersion,
            totalHeaderSize,
            thumbnailTableOffset,
            bulkDataStartOffset,
            names,
            imports,
            exports,
            registryObjects);
    }

    /**
     * Thumbnails saved in the package, read from the table at {@link UAssetPackage#thumbnailTableOffset()}.
     */
    public static List<UAssetThumbnail> readThumbnails(ByteBuffer buffer, UAssetPackage assetPackage) throws UAssetReadException {
        if (assetPackage.thumbnailTableOffset() <= 0) {
            return List.of();
        }

        UAssetArchive archive = new UAssetArchive(buffer);
        archive.seek(assetPackage.thumbnailTableOffset());
        int count = archive.readCount(12);
        List<String> classNames = new ArrayList<>(count);
        List<String> objectPaths = new ArrayList<>(count);
        List<Integer> offsets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            classNames.add(archive.readFString());
            objectPaths.add(archive.readFString());
            offsets.add(archive.readInt32());
        }

        List<UAssetThumbnail> thumbnails = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            archive.seek(offsets.get(i));
            int width = archive.readInt32();
            int height = archive.readInt32();
            // a negative height marks a JPEG image
            boolean jpeg = height < 0;
            height = Math.abs(height);
            byte[] imageData = new byte[0];
            if (width != 0 && height != 0) {
                imageData = archive.readBytes(archive.readCount(1));
            }
            thumbnails.add(new UAssetThumbnail(classNames.get(i), objectPaths.get(i), width, height, jpeg, imageData));
        }
        return thumbnails;
    }

    private static void skipCustomVersions(UAssetArchive archive, int legacyFileVersion) throws UAssetReadException {
        int count = archive.readCount(8);
        for (int i = 0; i < count; i++) {
            if (legacyFileVersion == -2) {
                // enum based: tag, version
                archive.skip(8);
            }
            else if (legacyFileVersion >= -5) {
                // guid based: key, version, friendly name
                archive.skip(GUID_SIZE + 4);
                archive.readFString();
            }
            else {
                // optimized: key, version
                archive.skip(GUID_SIZE + 4);
            }
        }
    }

    private static String readEngineVersion(UAssetArchive archive) throws UAssetReadException {
        int major = archive.readUInt16();
        int minor = archive.readUInt16();
        int patch = archive.readUInt16();
        int changelist = archive.readInt32();
        String branch = archive.readFString();
        if (major == 0 && minor == 0 && patch == 0) {
            return "";
        }
        StringBuilder version = new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
        if (changelist != 0) {
            version.append('-').append(Integer.toUnsignedString(changelist & 0x7FFFFFFF));
        }
        if (!branch.isEmpty()) {
            version.append('+').append(branch);
        }
        return version.toString();
    }

    private static List<String> readNames(UAssetArchive archive, int ue4, int count, int offset) throws UAssetReadException {
        if (count == 0) {
            return List.of();
        }
        if (count < 0 || count > archive.size() / 5) {
            throw new UAssetReadException("Invalid name count " + count);
        }
        archive.seek(offset);
        boolean hashes = ue4 >= VER_UE4_NAME_HASHES_SERIALIZED;
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add(archive.readFString());
            if (hashes) {
                // non case preserving hash, case preserving hash
                archive.skip(4);
            }
        }
        return names;
    }

    private static List<UAssetImport> readImports(UAssetArchive archive, int ue4, int ue5, int count, int offset)
        throws UAssetReadException {
        if (count == 0) {
            return List.of();
        }
        if (count < 0 || count > archive.size() / 28) {
            throw new UAssetReadException("Invalid import count " + count);
        }
        archive.seek(offset);
        List<UAssetImport> imports = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String classPackage = archive.readFName();
            String className = archive.readFName();
            int outerIndex = archive.readInt32();
            String objectName = archive.readFName();
            String packageName = ue4 >= VER_UE4_NON_OUTER_PACKAGE_IMPORT ? archive.readFName() : UAssetImport.NONE;
            if (ue5 >= VER_UE5_OPTIONAL_RESOURCES) {
                // import optional
                archive.skip(4);
            }
            imports.add(new UAssetImport(classPackage, className, outerIndex, objectName, packageName));
        }
        return imports;
    }

    private static List<UAssetExport> readExports(UAssetArchive archive, int ue4, int ue5, int packageFlags, int count, int offset)
        throws UAssetReadException {
        if (count == 0) {
            return List.of();
        }
        if (count < 0 || count > archive.size() / 40) {
            throw new UAssetReadException("Invalid export count " + count);
        }
        archive.seek(offset);
        boolean unversionedProperties = (packageFlags & UAssetPackage.PKG_UNVERSIONED_PROPERTIES) != 0;
        List<UAssetExport> exports = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int classIndex = archive.readInt32();
            int superIndex = archive.readInt32();
            int templateIndex = ue4 >= VER_UE4_TEMPLATE_INDEX_IN_COOKED_EXPORTS ? archive.readInt32() : 0;
            int outerIndex = archive.readInt32();
            String objectName = archive.readFName();
            int objectFlags = archive.readInt32();

            long serialSize;
            long serialOffset;
            if (ue4 < VER_UE4_64BIT_EXPORTMAP_SERIALSIZES) {
                serialSize = archive.readInt32();
                serialOffset = archive.readInt32();
            }
            else {
                serialSize = archive.readInt64();
                serialOffset = archive.readInt64();
            }

            // forced export, not for client, not for server
            archive.skip(12);
            if (ue5 < VER_UE5_REMOVE_OBJECT_EXPORT_PACKAGE_GUID) {
                // package guid
                archive.skip(GUID_SIZE);
            }
            if (ue5 >= VER_UE5_TRACK_OBJECT_EXPORT_IS_INHERITED) {
                // is inherited instance
                archive.skip(4);
            }
            // package flags
            archive.skip(4);
            if (ue4 >= VER_UE4_LOAD_FOR_EDITOR_GAME) {
                // not always loaded for editor game
                archive.skip(4);
            }
            boolean isAsset = ue4 >= VER_UE4_COOKED_ASSETS_IN_EDITOR_SUPPORT && archive.readBool();
            if (ue5 >= VER_UE5_OPTIONAL_RESOURCES) {
                // generate public hash
                archive.skip(4);
            }
            if (ue4 >= VER_UE4_PRELOAD_DEPENDENCIES_IN_COOKED_EXPORTS) {
                // first export dependency and four dependency counts
                archive.skip(20);
            }
            if (!unversionedProperties && ue5 >= VER_UE5_SCRIPT_SERIALIZATION_OFFSET) {
                // script serialization start, end offsets
                archive.skip(16);
            }

            exports.add(new UAssetExport(classIndex,
                superIndex,
                templateIndex,
                outerIndex,
                objectName,
                objectFlags,
                serialSize,
                serialOffset,
                isAsset));
        }
        return exports;
    }

    private static List<UAssetRegistryObject> readRegistryObjects(UAssetArchive archive, int ue4, int offset)
        throws UAssetReadException {
        if (offset <= 0) {
            return List.of();
        }
        archive.seek(offset);
        if (ue4 >= VER_UE4_ASSETREGISTRY_DEPENDENCYFLAGS) {
            // dependency data offset
            archive.skip(8);
        }
        int objectCount = archive.readCount(1);
        List<UAssetRegistryObject> objects = new ArrayList<>(objectCount);
        for (int i = 0; i < objectCount; i++) {
            String objectPath = archive.readFString();
            String className = archive.readFString();
            int tagCount = archive.readCount(8);
            Map<String, String> tags = new LinkedHashMap<>();
            for (int j = 0; j < tagCount; j++) {
                String key = archive.readFString();
                String value = archive.readFString();
                if (!key.isEmpty() && !value.isEmpty()) {
                    tags.put(key, value);
                }
            }
            objects.add(new UAssetRegistryObject(objectPath, className, tags));
        }
        return objects;
    }
}
