package mock.java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Set;

public class Mock {

  public static class DatagramSocket extends java.net.DatagramSocket {

    public InetSocketAddress address;

    public DatagramSocket(int port)
      throws SocketException, UnknownHostException {
      var addr = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
      this.address = new InetSocketAddress(addr, port);
    }

    public DatagramSocket(int port, InetAddress addr) throws SocketException {
      this.address = new InetSocketAddress(addr, port);
    }

    public InetSocketAddress address() {
      return this.address;
    }
  }

  public static class Channel
    extends DatagramChannel
    implements
      WritableByteChannel,
      GatheringByteChannel,
      ScatteringByteChannel,
      ReadableByteChannel {

    public Map<SocketAddress, List<ByteBuffer>> sent = new HashMap<>();
    public Map<SocketAddress, List<ByteBuffer>> recv = new HashMap<>();
    public List<Byte> bytes = new ArrayList<>();
    public DatagramSocket sock;

    public Channel() throws SocketException, UnknownHostException {
      this(new DatagramSocket(1234));
    }

    public Channel(DatagramSocket sock)
      throws SocketException, UnknownHostException {
      super(new SelectorProvider());
      this.sock = sock;
    }

    @Override
    public int send(ByteBuffer src, SocketAddress target) {
      var sent = this.sent.get(target);
      if (sent == null) {
        var list = new ArrayList<ByteBuffer>();
        this.sent.put(target, list);
      }

      this.sent.get(target).add(src);

      return (int) src.capacity();
    }

    @Override
    public SocketAddress receive(ByteBuffer dst) {
      for (Map.Entry<SocketAddress, List<ByteBuffer>> ent : this.recv.entrySet()) {
        if (ent.getValue().size() == 0) {
          this.recv.remove(ent.getKey());
        } else {
          var buf = ent.getValue().remove(0);
          dst.put(buf);
          return ent.getKey();
        }
      }
      return null;
    }

    @Override
    public int write(ByteBuffer src) {
      src.rewind();
      for (int j = 0; j < src.capacity(); j++) {
        this.bytes.add(src.get(j));
      }

      return (int) src.capacity();
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) {
      long writ = 0;
      for (ByteBuffer buf : srcs) {
        writ += this.write(buf);
      }
      return writ;
    }

    public int read(ByteBuffer dst, int start) {
      int orig = (int) dst.position();
      for (Byte b : this.bytes.subList(start, this.bytes.size())) {
        dst.put(b);
      }

      return (int) dst.position() - orig;
    }

    @Override
    public int read(ByteBuffer dst) {
      return this.read(dst, 0);
    }

    @Override
    public long read(ByteBuffer[] dsts, int off, int len) {
      long n = 0;
      for (ByteBuffer buf : dsts) {
        n += this.read(buf, (int) n);
      }
      return n;
    }

    @Override
    public void implConfigureBlocking(boolean b) {}

    @Override
    public void implCloseSelectableChannel() {}

    @Override
    public SocketAddress getLocalAddress() {
      return this.sock.address();
    }

    @Override
    public SocketAddress getRemoteAddress() {
      return null;
    }

    @Override
    public DatagramChannel disconnect() {
      return this;
    }

    @Override
    public DatagramChannel bind(SocketAddress local) {
      return this;
    }

    @Override
    public DatagramChannel connect(SocketAddress remote) {
      return this;
    }

    @Override
    public boolean isConnected() {
      return false;
    }

    @Override
    public DatagramSocket socket() {
      return this.sock;
    }

    @Override
    public MembershipKey join(InetAddress group, NetworkInterface interf) {
      throw new Error("unimplemented");
    }

    @Override
    public MembershipKey join(
      InetAddress group,
      NetworkInterface interf,
      InetAddress source
    ) {
      throw new Error("unimplemented");
    }

    @Override
    public <T> DatagramChannel setOption(SocketOption<T> name, T value) {
      return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) {
      throw new Error("unimplemented");
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
      throw new Error("unimplemented");
    }
  }

  public static class Pipe extends java.nio.channels.Pipe {

    public SinkChannel sink;
    public SourceChannel source;

    public Pipe() throws SocketException, UnknownHostException {
      this.sink = new SinkChannel();
      this.source = new SourceChannel();
    }

    @Override
    public SinkChannel sink() {
      return this.sink;
    }

    @Override
    public SourceChannel source() {
      return this.source;
    }

    public static class SinkChannel
      extends java.nio.channels.Pipe.SinkChannel
      implements WritableByteChannel, GatheringByteChannel {

      public SinkChannel() throws SocketException, UnknownHostException {
        super(new SelectorProvider());
        this.channel = new Channel();
      }

      public Channel channel;

      @Override
      public void implCloseSelectableChannel() {}

      @Override
      public void implConfigureBlocking(boolean block) {}

      @Override
      public int write(ByteBuffer src) {
        return this.channel.write(src);
      }

      @Override
      public long write(ByteBuffer[] srcs) throws IOException {
        return this.channel.write(srcs);
      }

      @Override
      public long write(ByteBuffer[] srcs, int offset, int length) {
        return this.channel.write(srcs, offset, length);
      }
    }

    public static class SourceChannel
      extends java.nio.channels.Pipe.SourceChannel {

      public Channel channel;

      public SourceChannel() throws SocketException, UnknownHostException {
        super(new SelectorProvider());
        this.channel = new Channel();
      }

      @Override
      public void implCloseSelectableChannel() {}

      @Override
      public void implConfigureBlocking(boolean block) {}

      @Override
      public int read(ByteBuffer buf) {
        return this.channel.read(buf);
      }

      @Override
      public long read(ByteBuffer[] dsts) throws IOException {
        return this.channel.read(dsts);
      }

      @Override
      public long read(ByteBuffer[] dsts, int off, int len) {
        return this.channel.read(dsts, off, len);
      }
    }
  }

  public static class SelectionKey extends AbstractSelectionKey {

    public SelectionKey() {}

    @Override
    public int readyOps() {
      return 0;
    }

    @Override
    public int interestOps() {
      return 0;
    }

    @Override
    public java.nio.channels.SelectionKey interestOps(int o) {
      return this;
    }

    @Override
    public java.nio.channels.Selector selector() {
      return new Selector();
    }

    @Override
    public java.nio.channels.SelectableChannel channel() {
      try {
        return new Channel();
      } catch (UnknownHostException e) {
        throw new Error(e);
      } catch (SocketException e) {
        throw new Error(e);
      }
    }
  }

  public static class Selector extends AbstractSelector {

    public Selector() {
      super(new SelectorProvider());
    }

    @Override
    public void implCloseSelector() {}

    @Override
    public java.nio.channels.SelectionKey register(
      AbstractSelectableChannel ch,
      int ops,
      Object att
    ) {
      return new SelectionKey();
    }

    @Override
    public java.nio.channels.Selector wakeup() {
      return this;
    }

    @Override
    public int select(long timeout) {
      return 0;
    }

    @Override
    public int select() {
      return 0;
    }

    @Override
    public int selectNow() {
      return 0;
    }

    @Override
    public Set<java.nio.channels.SelectionKey> selectedKeys() {
      throw new Error("unimplemented");
    }

    @Override
    public Set<java.nio.channels.SelectionKey> keys() {
      throw new Error("unimplemented");
    }
  }

  public static class SelectorProvider
    extends java.nio.channels.spi.SelectorProvider {

    @Override
    public java.nio.channels.DatagramChannel openDatagramChannel() {
      return this.openDatagramChannel(StandardProtocolFamily.INET);
    }

    @Override
    public java.nio.channels.DatagramChannel openDatagramChannel(
      ProtocolFamily proto
    ) {
      throw new Error("unimplemented");
    }

    @Override
    public SocketChannel openSocketChannel() {
      throw new Error("unimplemented");
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() {
      throw new Error("unimplemented");
    }

    @Override
    public AbstractSelector openSelector() {
      return new Selector();
    }

    @Override
    public Pipe openPipe() throws SocketException, UnknownHostException {
      return new Pipe();
    }
  }
}
