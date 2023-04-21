import mock.java.nio.channels.Mock

import java.nio.ByteBuffer
import java.net.InetAddress
import java.net.InetSocketAddress

class MockChannel extends munit.FunSuite {
  test("channel pair works") {
    val a = Mock.Channel(1)
    val b = Mock.Channel(2)
    a.pairTo(b)

    a.send(
      ByteBuffer.wrap(Array[Byte](1, 2, 3)),
      InetSocketAddress(InetAddress.getByAddress(Array[Byte](0, 0, 0, 0)), 1)
    )
    a.send(
      ByteBuffer.wrap(Array[Byte](2, 3, 4)),
      InetSocketAddress(InetAddress.getByAddress(Array[Byte](0, 0, 0, 0)), 1)
    )

    0.until(2).foreach { n =>
      val into = ByteBuffer.allocate(3)
      val addr = b.receive(into)
      into.rewind();

      val recvd = Array[Byte](0, 0, 0)
      into.get(recvd)

      assert(addr != null)
      assertEquals(
        recvd.toSeq,
        Seq(1, 2, 3).map(i => i + n).map(i => i.byteValue)
      )
    }
  }
}
