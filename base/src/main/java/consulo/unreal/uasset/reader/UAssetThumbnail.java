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
 * The Content Browser preview of one object, as the editor saved it into the package.
 *
 * @param objectClassName  class of the object, e.g. {@code StaticMesh}
 * @param objectPath       object path without the package name, e.g. {@code Cube}
 * @param width            image width, {@code 0} when the editor saved an empty thumbnail
 * @param height           image height
 * @param jpeg             {@code imageData} is a JPEG file, otherwise a PNG file
 * @param imageData        the compressed image, readable by {@code javax.imageio}
 *
 * @author VISTALL
 * @since 2026-11-24
 */
public record UAssetThumbnail(String objectClassName, String objectPath, int width, int height, boolean jpeg, byte[] imageData) {
}
