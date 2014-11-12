package ethanjones.cubes.networking.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.net.NetJavaSocketImpl;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;

import ethanjones.cubes.core.logging.Log;
import ethanjones.cubes.core.platform.Adapter;
import ethanjones.cubes.core.system.Branding;
import ethanjones.cubes.networking.Networking;
import ethanjones.cubes.networking.NetworkingManager;
import ethanjones.cubes.networking.packet.Packet;
import ethanjones.cubes.networking.packets.PacketConnect;
import ethanjones.cubes.networking.packets.PacketPlayerInfo;
import ethanjones.cubes.networking.socket.SocketMonitor;
import ethanjones.cubes.side.Side;
import ethanjones.cubes.side.common.Cubes;

public class ClientNetworking extends Networking {

  private final ClientNetworkingParameter clientNetworkingParameter;
  private final String host;
  private final int port;
  private SocketMonitor socketMonitor;
  private Socket socket;

  private Vector3 prevPosition = new Vector3();
  private Vector3 prevDirection = new Vector3();

  public ClientNetworking(ClientNetworkingParameter clientNetworkingParameter) {
    super(Side.Client);
    this.clientNetworkingParameter = clientNetworkingParameter;
    this.host = clientNetworkingParameter.host;
    this.port = clientNetworkingParameter.port;
  }

  public synchronized void preInit() throws Exception {
    setNetworkingState(NetworkingState.PreInit);
    Log.info("Starting Client Networking");
    Socket socket;
    try {
      socket = Gdx.net.newClientSocket(NetworkingManager.protocol, host, port, NetworkingManager.socketHints);
    } catch (GdxRuntimeException e) {
      if (!(e.getCause() instanceof Exception)) throw e;
      throw (Exception) e.getCause();
    }
    java.net.Socket javaSocket;
    if (socket instanceof NetJavaSocketImpl) {
      try {
        Field f = NetJavaSocketImpl.class.getDeclaredField("socket");
        f.setAccessible(true);
        javaSocket = (java.net.Socket) f.get(socket);
      } catch (Exception e) {
        throw new IOException("Failed to get java socket", e);
      }
    } else {
      throw new IOException("libGDX socket is not a " + NetJavaSocketImpl.class.getSimpleName());
    }
    javaSocket.setSoTimeout(500);
    int serverMajor;
    int serverMinor;
    int serverPoint;
    int serverBuild;
    String serverHash;
    try {
      DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
      serverMajor = dataInputStream.readInt();
      serverMinor = dataInputStream.readInt();
      serverPoint = dataInputStream.readInt();
      serverBuild = dataInputStream.readInt();
      serverHash = dataInputStream.readUTF();
    } catch (IOException e) {
      if (e instanceof SocketTimeoutException) {
        throw new IOException("Server did not respond in time", e);
      } else {
        throw e;
      }
    }
    if (serverMajor == Branding.VERSION_MAJOR && serverMinor == Branding.VERSION_MINOR && serverPoint == Branding.VERSION_POINT) {
      if (serverBuild == Branding.VERSION_BUILD) {
        if (serverHash != Branding.VERSION_HASH) {
          Log.warning("Server reports the same build, but has a different hash");
        } else {
          Log.debug("Server is running exactly the same build");
        }
      } else {
        Log.warning("Server is running build " + serverBuild);
      }
    } else {
      String str = serverMajor + "." + serverMinor + "." + serverPoint;
      throw new IOException("Server is running version " + str + " not " + Branding.VERSION_MAJOR_MINOR_POINT);
    }
    javaSocket.setSoTimeout(0);
    this.socket = socket;
    Log.info("Successfully connected to " + host + ":" + port);
  }

  @Override
  public void init() {
    setNetworkingState(NetworkingState.Init);
    socketMonitor = new SocketMonitor(socket, this);
    sendToServer(new PacketConnect());
    setNetworkingState(NetworkingState.Running);
  }

  @Override
  public synchronized void tick() {
    if (getNetworkingState() != NetworkingState.Running) Adapter.gotoMainMenu();

    if (!Cubes.getClient().player.position.equals(prevPosition) || !Cubes.getClient().player.angle.equals(prevDirection)) {
      PacketPlayerInfo packetPlayerInfo = new PacketPlayerInfo();
      packetPlayerInfo.angle = Cubes.getClient().player.angle;
      packetPlayerInfo.position = Cubes.getClient().player.position;
      sendToServer(packetPlayerInfo);
      prevPosition.set(Cubes.getClient().player.position);
      prevDirection.set(Cubes.getClient().player.angle);
    }
  }

  @Override
  public synchronized void stop() {
    if (getNetworkingState() != NetworkingState.Running) return;
    setNetworkingState(NetworkingState.Stopping);
    Log.info("Stopping Client Networking");
    socketMonitor.dispose();
    setNetworkingState(NetworkingState.Stopped);
  }

  @Override
  public synchronized void disconnected(SocketMonitor socketMonitor, Exception e) {
    if (getNetworkingState() == NetworkingState.Stopping || getNetworkingState() == NetworkingState.Stopped) return;
    if (NetworkingManager.serverNetworking != null && (NetworkingManager.serverNetworking.getNetworkingState() == NetworkingState.Stopping || NetworkingManager.serverNetworking.getNetworkingState() == NetworkingState.Stopped)) {
      return;
    }
    Log.info("Disconnected from " + socketMonitor.getRemoteAddress(), e);
    stop();
  }

  public synchronized void sendToServer(Packet packet) {
    socketMonitor.queue(packet);
  }
}
