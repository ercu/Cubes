package ethanjones.cubes.core.platform.desktop;

import ethanjones.cubes.core.mod.ModLoader;

import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class DesktopModLoader implements ModLoader {

  public static class ExternalJarLoader extends URLClassLoader {

    public ExternalJarLoader(FileHandle fileHandle) throws IOException {
      super(new URL[]{fileHandle.file().toURI().toURL()});
    }

  }

  @Override
  public ModType getType() {
    return ModType.jar;
  }

  @Override
  public Class<?> loadClass(FileHandle classFile, String className) throws Exception {
    return new ExternalJarLoader(classFile).loadClass(className);
  }
}
