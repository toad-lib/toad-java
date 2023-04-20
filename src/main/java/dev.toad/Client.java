package dev.toad;

import dev.toad.msg.Code;
import dev.toad.msg.Message;
import dev.toad.msg.Payload;
import dev.toad.msg.Token;
import dev.toad.msg.Type;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class Client implements AutoCloseable {

  final Toad toad;

  Client(Toad toad) {
    this.toad = toad;
  }

  public CompletableFuture<Message> get(String uri)
    throws URISyntaxException, UnknownHostException {
    return this.get(Type.CON, uri);
  }

  public CompletableFuture<Message> get(Type ty, String uri)
    throws URISyntaxException, UnknownHostException {
    return this.get(ty, uri, new Payload());
  }

  public CompletableFuture<Message> get(Type ty, String uri, Payload p)
    throws URISyntaxException, UnknownHostException {
    return this.send(
        Message.builder().uri(uri).type(ty).code(Code.GET).payload(p).build()
      );
  }

  public CompletableFuture<Message> post(String uri)
    throws URISyntaxException, UnknownHostException {
    return this.post(Type.CON, uri);
  }

  public CompletableFuture<Message> post(Type ty, String uri)
    throws URISyntaxException, UnknownHostException {
    return this.post(ty, uri, new Payload());
  }

  public CompletableFuture<Message> post(Type ty, String uri, Payload p)
    throws URISyntaxException, UnknownHostException {
    return this.send(
        Message.builder().uri(uri).type(ty).code(Code.POST).payload(p).build()
      );
  }

  public CompletableFuture<Message> put(String uri)
    throws URISyntaxException, UnknownHostException {
    return this.put(Type.CON, uri);
  }

  public CompletableFuture<Message> put(Type ty, String uri)
    throws URISyntaxException, UnknownHostException {
    return this.put(ty, uri, new Payload());
  }

  public CompletableFuture<Message> put(Type ty, String uri, Payload p)
    throws URISyntaxException, UnknownHostException {
    return this.send(
        Message.builder().uri(uri).type(ty).code(Code.PUT).payload(p).build()
      );
  }

  public CompletableFuture<Message> delete(String uri)
    throws URISyntaxException, UnknownHostException {
    return this.delete(Type.CON, uri);
  }

  public CompletableFuture<Message> delete(Type ty, String uri)
    throws URISyntaxException, UnknownHostException {
    return this.delete(ty, uri, new Payload());
  }

  public CompletableFuture<Message> delete(Type ty, String uri, Payload p)
    throws URISyntaxException, UnknownHostException {
    return this.send(
        Message.builder().uri(uri).type(ty).code(Code.DELETE).payload(p).build()
      );
  }

  public ClientObserveStream observe(Message message) {
    return new ClientObserveStream(this, message);
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
        this.awaitResponse(sent.token, message.addr().get())
      );
  }

  public CompletableFuture<Message> awaitResponse(
    Token t,
    InetSocketAddress addr
  ) {
    return Async
      .pollCompletable(() -> this.toad.pollResp(t, addr))
      .thenApply(msg -> msg.toOwned());
  }

  @Override
  public void close() {
    this.toad.close();
  }
}
