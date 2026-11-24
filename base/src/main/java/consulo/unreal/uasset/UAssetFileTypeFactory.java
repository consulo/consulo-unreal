package consulo.unreal.uasset;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;

/**
 * @author VISTALL
 * @since 2026-11-24
 */
@ExtensionImpl
public class UAssetFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(FileTypeConsumer consumer) {
        consumer.consume(UAssetFileType.INSTANCE);
    }
}
