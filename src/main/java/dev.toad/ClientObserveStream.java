package dev.toad;

import dev.toad.Async;
import dev.toad.msg.Message;
import dev.toad.msg.Token;
import dev.toad.msg.option.Observe;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ClientObserveStream implements AutoCloseable {

  boolean closed;
  CompletableFuture<Message> initial;
  Optional<Token> token = Optional.empty();
  final Client client;
  final Message message;

  public ClientObserveStream(Client client, Message message) {
    this.closed = false;
    this.client = client;
    this.message = message.buildCopy().option(Observe.REGISTER).build();
    this.initial = client.send(this.message);
  }

  public Async.LoopHandle subscribe(Consumer<Message> event) {
    return Async.loop(
      () -> {
        try {
          event.accept(this.next().get());
        } catch (ExecutionException | InterruptedException e) {
          throw new RuntimeException(e);
        }
      },
      () -> {
        this.close();
      }
    );
  }

  public CompletableFuture<Message> next() {
    if (this.closed) {
      throw new RuntimeException(
        "ClientObserveStream.next() invoked after .close()"
      );
    } else if (this.token.isEmpty()) {
      return this.initial.whenComplete((rep, e) -> {
          if (rep != null) {
            this.token = Optional.of(rep.token());
          }
        });
    } else {
      return this.initial.thenCompose(_i ->
          this.client.awaitResponse(this.token.get(), this.message.addr().get())
        );
    }
  }

  @Override
  public void close() {
    try {
      this.client.send(
          this.message.buildCopy().option(Observe.DEREGISTER).unsetId().build()
        )
        .get();
    } catch (Throwable t) {}

    this.closed = true;
  }
}
