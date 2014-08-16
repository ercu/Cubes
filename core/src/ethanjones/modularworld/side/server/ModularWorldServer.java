package ethanjones.modularworld.side.server;

import ethanjones.modularworld.block.factory.BlockFactories;
import ethanjones.modularworld.core.timing.TimeHandler;
import ethanjones.modularworld.networking.NetworkingManager;
import ethanjones.modularworld.networking.common.socket.SocketMonitor;
import ethanjones.modularworld.networking.server.ServerNetworkingParameter;
import ethanjones.modularworld.side.Side;
import ethanjones.modularworld.side.common.ModularWorld;
import ethanjones.modularworld.world.WorldServer;
import ethanjones.modularworld.world.generator.BasicWorldGenerator;

import java.util.HashMap;

public class ModularWorldServer extends ModularWorld implements TimeHandler {

  public static ModularWorldServer instance;
  private final ServerNetworkingParameter serverNetworkingParameter;
  public HashMap<SocketMonitor, PlayerManager> playerManagers;
  private boolean disposed = false;

  public ModularWorldServer(ServerNetworkingParameter serverNetworkingParameter) {
    super(Side.Server);
    this.serverNetworkingParameter = serverNetworkingParameter;
    ModularWorldServer.instance = this;
    playerManagers = new HashMap<SocketMonitor, PlayerManager>();
  }

  @Override
  public void create() {
    super.create();
    NetworkingManager.startServer(serverNetworkingParameter);

    world = new WorldServer(new BasicWorldGenerator());

    timing.addHandler(this, 250);
  }

  @Override
  public void dispose() {
    if (disposed) return;
    super.dispose();
    disposed = true;
  }

  @Override
  public void time(int interval) {
    world.setBlockFactory(BlockFactories.dirt, (int) (Math.random() * 16), (int) (8 + (Math.random() * 7)), (int) (Math.random() * 16));
  }
}
