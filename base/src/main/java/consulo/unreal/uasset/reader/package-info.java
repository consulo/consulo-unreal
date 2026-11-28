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

/**
 * Reader of the header of {@code .uasset} / {@code .umap} files: versions, name, import and export tables, asset registry
 * tags and the thumbnails the editor saved.
 * <p>
 * For future use - nothing uses it yet. It is meant for an index of assets (asset class, Blueprint parent class, the
 * {@code /Script/...} classes an asset references) and a read-only viewer of {@code .uasset} files. It parses all
 * 17,451 assets of the UE 5.8 engine content (UE4 versions 516-522, UE5 versions 1002-1018).
 */
package consulo.unreal.uasset.reader;
