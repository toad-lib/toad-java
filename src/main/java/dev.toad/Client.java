package dev.toad;

import dev.toad.msg.Code;
import dev.toad.msg.Type;
import dev.toad.msg.Message;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class Client implements AutoCloseable {

  final Toad toad;

  Client(Toad toad) {
    this.toad = toad;
  }

  public CompletableFuture<Message> get(String uri) throws URISyntaxException, UnknownHostException {
    return this.get(Type.CON, uri);
  }

  public CompletableFuture<Message> get(Type ty, String uri) throws URISyntaxException, UnknownHostException {
    return this.send(Message.builder().uri(uri).type(ty).code(Code.GET).build());
  }

  public CompletableFuture<Message> send(Message message) {
    if (message.addr().isEmpty()) {
      throw new IllegalArgumentException(
        "Message destination address must be set"
      );
    }

    return Async
      .pollCompletable(() -> this.toad.sendMessage(message))
      .thenCompose((Toad.IdAndToken sent) ->
        Async.pollCompletable(() ->
          this.toad.pollResp(sent.token, message.addr().get())
        )
      )
      .thenApply(msg -> msg.toOwned());
  }

  @Override
  public void close() {
    this.toad.close();
  }
}
