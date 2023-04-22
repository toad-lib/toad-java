package dev.toad;

import dev.toad.msg.Message;
import dev.toad.msg.Token;
import dev.toad.msg.option.Observe;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ClientObserveStream implements AutoCloseable {

  State state;
  CompletableFuture<Message> initial;
  Optional<Token> token = Optional.empty();
  final Client client;
  final Message message;

  public ClientObserveStream(Client client, Message message) {
    this.state = State.OPEN;
    this.client = client;
    this.message = message.buildCopy().option(Observe.REGISTER).build();
    this.initial = client.send(this.message);
  }

  public CompletableFuture<Message> next() {
    if (State.eq.test(State.CLOSED, this.state)) {
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

    this.state = State.CLOSED;
  }

  public static final class State {

    public static final Eq<State> eq = Eq.int_.contramap((State s) -> s.state);

    public static final State OPEN = new State(0);
    public static final State CLOSED = new State(1);

    final int state;

    State(int state) {
      this.state = state;
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case State s -> State.eq.test(this, s);
        default -> false;
      };
    }
  }
}
