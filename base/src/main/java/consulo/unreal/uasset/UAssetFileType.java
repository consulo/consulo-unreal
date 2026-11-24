package consulo.unreal.uasset;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.unreal.icon.UnrealIconGroup;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * @author VISTALL
 * @since 2026-11-24
 */
public class UAssetFileType implements FileType {
    public static final UAssetFileType INSTANCE = new UAssetFileType();

    public static final String EXTENSION = "uasset";

    @Override
    public String getId() {
        return "UASSET";
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Unreal Asset File");
    }

    @Override
    public Image getIcon() {
        return UnrealIconGroup.unreal();
    }

    @Override
    public String getDefaultExtension() {
        return EXTENSION;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
