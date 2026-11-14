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

package consulo.unreal.project;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Include paths, defines and C++ standard of one module, collected while reading what UnrealBuildTool produces.
 */
public final class UnrealCompileSettings {
    private final Set<String> myIncludePaths = new LinkedHashSet<>();
    private final Map<String, String> myDefines = new LinkedHashMap<>();
    private @Nullable String myCppStandard;

    public void addIncludePath(String path) {
        myIncludePaths.add(path);
    }

    public void define(String name, String value) {
        myDefines.put(name, value);
    }

    public void undefine(String name) {
        myDefines.remove(name);
    }

    public void setCppStandard(@Nullable String cppStandard) {
        myCppStandard = cppStandard;
    }

    public List<String> getIncludePaths() {
        return List.copyOf(myIncludePaths);
    }

    public Map<String, String> getDefines() {
        return myDefines;
    }

    /**
     * @return the value of {@code -std=}, e.g. {@code c++20}
     */
    public @Nullable String getCppStandard() {
        return myCppStandard;
    }

    public void addAll(UnrealCompileSettings settings) {
        myIncludePaths.addAll(settings.myIncludePaths);
        settings.myDefines.forEach(myDefines::putIfAbsent);
        if (myCppStandard == null) {
            myCppStandard = settings.myCppStandard;
        }
    }
}
