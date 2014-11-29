package ethanjones.cubes.core.platform.desktop;

public class ClientLauncher implements DesktopLauncher {

  private final String[] arg;

  private ClientLauncher(String[] arg) {
    this.arg = arg;
  }

  public static void main(String[] arg) {
    new ClientLauncher(arg).start();
  }

  private void start() {
    DesktopSecurityManager.setup();
    new ClientCompatibility(this, arg).startCubes();
  }
}