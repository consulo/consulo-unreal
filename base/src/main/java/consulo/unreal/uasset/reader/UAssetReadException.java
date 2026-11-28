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

import java.io.IOException;

/**
 * The file is not a package, is corrupt, or uses a format this reader does not support (cooked, unversioned or too new).
 *
 * @author VISTALL
 * @since 2026-11-24
 */
public class UAssetReadException extends IOException {
    public UAssetReadException(String message) {
        super(message);
    }
}
