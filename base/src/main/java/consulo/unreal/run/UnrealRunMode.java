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

import consulo.localize.LocalizeValue;
import consulo.unreal.localize.UnrealLocalize;

/**
 * What an Unreal run configuration starts.
 */
public enum UnrealRunMode {
    /**
     * The Unreal Editor with the project.
     */
    EDITOR,
    /**
     * The project as a game, in the editor executable ({@code -game}) - no cooking or packaging needed.
     */
    GAME,
    /**
     * The executable of a game target built by UnrealBuildTool ({@code Binaries/<Platform>/<Target>}).
     */
    STANDALONE;

    public LocalizeValue getPresentableName() {
        return switch (this) {
            case EDITOR -> UnrealLocalize.runModeEditor();
            case GAME -> UnrealLocalize.runModeGame();
            case STANDALONE -> UnrealLocalize.runModeStandalone();
        };
    }
}
