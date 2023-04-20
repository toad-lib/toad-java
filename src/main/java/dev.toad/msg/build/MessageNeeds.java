package dev.toad.msg.build;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;

public final class MessageNeeds {

  public interface Destination {
    MessageNeeds.Type uri(String uri) throws URISyntaxException, UnknownHostException;
    MessageNeeds.Type addr(InetSocketAddress addr);
  }

  public interface Type {
    MessageNeeds.Code type(dev.toad.msg.Type type);
  }

  public interface Code {
    Message code(dev.toad.msg.Code code);
  }
}
