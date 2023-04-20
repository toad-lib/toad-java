package dev.toad;

import dev.toad.msg.Message;
import dev.toad.msg.option.Observe;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ClientObserveStream {

  State state;
  Optional<CompletableFuture<Message>> buffered;
  final Client client;
  final Message message;

  public ClientObserveStream(Client client, Message message) {
    this.state = State.OPEN;
    this.client = client;
    this.message = message.modify().option(Observe.REGISTER).build();
    this.buffered = Optional.of(client.send(this.message));
  }

  public CompletableFuture<Void> close() {
    return this.client.send(
        this.message.modify().option(Observe.DEREGISTER).unsetId().build()
      )
      .thenAccept(m -> {
        this.state = State.CLOSED;
      });
  }

  public CompletableFuture<Message> next() {
    if (this.state == State.CLOSED) {
      throw new RuntimeException(
        "ClientObserveStream.next() invoked after .close()"
      );
    } else if (this.buffered.isEmpty()) {
      var buffered = this.buffered.get();
      this.buffered = Optional.empty();
      return buffered;
    } else {
      return this.client.awaitResponse(
          this.message.token(),
          this.message.addr().get()
        );
    }
  }

  public static final class State {

    public static final State OPEN = new State(0);
    public static final State CLOSED = new State(1);

    final int state;

    State(int state) {
      this.state = state;
    }

    public boolean equals(State other) {
      return this.state == other.state;
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case State s -> this.equals(s);
        default -> false;
      };
    }
  }
}
