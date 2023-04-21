package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import dev.toad.ffi.u8;
import java.util.List;

public final class Code implements Debug {

  final u8 clazz;
  final u8 detail;

  public static Eq<Code> eq = Eq.all(
    List.of(
      Eq.short_.contramap(Code::codeClass),
      Eq.short_.contramap(Code::codeDetail)
    )
  );

  public static final Code EMPTY = new Code(0, 0);

  public static final Code GET = new Code(0, 1);
  public static final Code POST = new Code(0, 2);
  public static final Code PUT = new Code(0, 3);
  public static final Code DELETE = new Code(0, 4);

  public static final Code OK_CREATED = new Code(2, 1);
  public static final Code OK_DELETED = new Code(2, 2);
  public static final Code OK_VALID = new Code(2, 3);
  public static final Code OK_CHANGED = new Code(2, 4);
  public static final Code OK_CONTENT = new Code(2, 5);

  public static final Code BAD_REQUEST = new Code(4, 0);
  public static final Code UNAUTHORIZED = new Code(4, 1);
  public static final Code BAD_OPTION = new Code(4, 2);
  public static final Code FORBIDDEN = new Code(4, 3);
  public static final Code NOT_FOUND = new Code(4, 4);
  public static final Code METHOD_NOT_ALLOWED = new Code(4, 5);
  public static final Code NOT_ACCEPTABLE = new Code(4, 6);
  public static final Code PRECONDITION_FAILED = new Code(4, 12);
  public static final Code REQUEST_ENTITY_TOO_LARGE = new Code(4, 13);
  public static final Code UNSUPPORTED_CONTENT_FORMAT = new Code(4, 15);

  public static final Code INTERNAL_SERVER_ERROR = new Code(5, 0);
  public static final Code NOT_IMPLEMENTED = new Code(5, 1);
  public static final Code BAD_GATEWAY = new Code(5, 2);
  public static final Code SERVICE_UNAVAILABLE = new Code(5, 3);
  public static final Code GATEWAY_TIMEOUT = new Code(5, 4);
  public static final Code PROXYING_NOT_SUPPORTED = new Code(5, 5);

  public Code(int clazz, int detail) {
    this.clazz = new u8((short) clazz);
    this.detail = new u8((short) detail);
  }

  public short codeClass() {
    return this.clazz.shortValue();
  }

  public short codeDetail() {
    return this.detail.shortValue();
  }

  @Override
  public String toString() {
    if (this.isRequest()) {
      return switch ((Short) this.detail.shortValue()) {
        case 1 -> "GET";
        case 2 -> "POST";
        case 3 -> "PUT";
        case Short other -> "DELETE";
      };
    } else {
      var str = String.format(
        "%d.%d",
        this.clazz.shortValue(),
        this.detail.shortValue()
      );

      return switch (str) {
        case "2.01" -> "2.01 Created";
        case "2.02" -> "2.02 Deleted";
        case "2.03" -> "2.03 Valid";
        case "2.04" -> "2.04 Changed";
        case "2.05" -> "2.05 Content";
        case "4.00" -> "4.00 Bad Request";
        case "4.01" -> "4.01 Unauthorized";
        case "4.02" -> "4.02 Bad Option";
        case "4.03" -> "4.03 Forbidden";
        case "4.04" -> "4.04 Not Found";
        case "4.05" -> "4.05 Method Not Allowed";
        case "4.06" -> "4.06 Not Acceptable";
        case "4.12" -> "4.12 Precondition Failed";
        case "4.13" -> "4.13 Request Entity Too Large";
        case "4.15" -> "4.15 Unsupported Content Format";
        case "5.00" -> "5.00 Internal Server Error";
        case "5.01" -> "5.01 Not Implemented";
        case "5.02" -> "5.02 Bad Gateway";
        case "5.03" -> "5.03 Service Unavailable";
        case "5.04" -> "5.04 Gateway Timeout";
        case "5.05" -> "5.05 Proxying Not Supported";
        case String other -> other;
      };
    }
  }

  public boolean isRequest() {
    return this.codeClass() == 0 && this.codeDetail() > 0;
  }

  public boolean isResponse() {
    return this.codeClass() > 1;
  }

  public boolean isEmpty() {
    return this.codeClass() == 0 && this.codeDetail() == 0;
  }

  @Override
  public String toDebugString() {
    return this.toString();
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Code c -> Code.eq.test(c, this);
      default -> false;
    };
  }
}
