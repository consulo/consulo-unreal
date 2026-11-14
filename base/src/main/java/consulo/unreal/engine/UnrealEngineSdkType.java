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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.content.bundle.PlatformAwareSdkType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.content.bundle.SdkType;
import consulo.platform.Platform;
import consulo.unreal.icon.UnrealIconGroup;
import consulo.unreal.localize.UnrealLocalize;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Unreal Engine bundle. Its home is the directory holding {@code Engine/} - see {@link UnrealEngine}.
 */
@ExtensionImpl
public class UnrealEngineSdkType extends PlatformAwareSdkType {
    public static final String ID = "UNREAL_ENGINE";

    public static UnrealEngineSdkType getInstance() {
        return Application.get().getExtensionPoint(SdkType.class).findExtensionOrFail(UnrealEngineSdkType.class);
    }

    /**
     * The engine bundle with the given home, added to the bundles when the engine was found another way (registered by the
     * launcher, the engine tree around the project). Must be called in a write action.
     */
    public static @Nullable Sdk findOrCreateSdk(Path home) {
        SdkTable sdkTable = SdkTable.getInstance();
        UnrealEngineSdkType sdkType = getInstance();
        for (Sdk sdk : sdkTable.getSdksOfType(sdkType)) {
            String sdkHome = sdk.getHomePath();
            if (sdkHome != null && Path.of(sdkHome).equals(home)) {
                return sdk;
            }
        }
        if (!UnrealEngine.isValidHome(home)) {
            return null;
        }
        Platform platform = Platform.current();
        Sdk sdk = sdkTable.createSdk(home, sdkType.suggestSdkName(platform, null, home), sdkType);
        sdkTable.addSdk(sdk);
        return sdk;
    }

    public UnrealEngineSdkType() {
        super(ID, UnrealLocalize.sdkTypeName(), UnrealIconGroup.unreal());
    }

    @Override
    public void collectHomePaths(Platform platform, Consumer<Path> consumer) {
        UnrealEngineLocator.collectHomePaths(platform).forEach(consumer);
    }

    @Override
    public boolean canCreatePredefinedSdks(Platform platform) {
        return Platform.LOCAL.equals(platform.getId());
    }

    @Override
    public boolean isValidSdkHome(Platform platform, Path path) {
        return UnrealEngine.isValidHome(path);
    }

    /**
     * Accepts the {@code Engine} directory itself as well as its parent.
     */
    @Override
    public Path adjustSelectedSdkHome(Platform platform, Path homePath) {
        Path parent = homePath.getParent();
        if (!UnrealEngine.isValidHome(homePath) && parent != null && UnrealEngine.isValidHome(parent)) {
            return parent;
        }
        return homePath;
    }

    @Override
    public @Nullable String getVersionString(Platform platform, Path path) {
        return UnrealEngine.isValidHome(path) ? new UnrealEngine(platform, path).readVersion() : null;
    }

    @Override
    public String suggestSdkName(Platform platform, @Nullable String currentSdkName, Path path) {
        String version = getVersionString(platform, path);
        return version == null ? "Unreal Engine" : "Unreal Engine " + version;
    }
}
