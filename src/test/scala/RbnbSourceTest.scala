import org.scalatest._
import org.cardioart.rdv.parser._

class RbnbSourceTest extends WordSpec {
  "A RbnbSource" should {
    "be None when input empty string" in {
      "" match {
        case RbnbSource(host, port, endpoint, channel) => fail("empty string cannot be extracted")
        case _ => ()
      }
    }
    "be None when input `sometext`" in {
      "sometext" match {
        case RbnbSource(host, port, endpoint, channel) => fail("a single word cannot be extracted")
        case _ => ()
      }
    }

    "be None when input `/`" in {
      "/" match {
        case RbnbSource(host, port, endpoint, channel) => fail("a single word cannot be extracted")
        case _ => ()
      }
    }

    "be (,,endpoint_1,) when input `/endpoint_1`" in {
      "/endpoint_1" match {
        case RbnbSource(host, port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "endpoint_1")
          assert(channel == "")
        case _ => fail("extractor returns None")
      }
    }

    "be (,,endpoint_1,) when input `endpoint_1/`" in {
      "endpoint_1/" match {
        case RbnbSource(host, port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "endpoint_1")
          assert(channel == "")
        case _ => fail("extractor returns None")
      }
    }

    "be (host,port,,) when input `host:port/`" in {
      "host:port/" match {
        case RbnbSource(host, port, endpoint, channel) =>
          assert(host == "host")
          assert(port == "port")
          assert(endpoint == "")
          assert(channel == "")
        case _ => fail("extractor returns None")
      }
    }

    "be (host,port, ep, ch/a/b/c) when input `host:port/ep/ch/a/b/c`" in {
      "host:port/ep/ch/a/b/c" match {
        case RbnbSource(host, port, endpoint, channel) =>
          assert(host == "host")
          assert(port == "port")
          assert(endpoint == "ep")
          assert(channel == "ch/a/b/c")
        case _ => fail("extractor return None")
      }
    }

    "be (host,port,,) when input `host:port`" in {
      "host:port" match {
        case RbnbSource(host, port, endpoint, channel) =>
          assert(host == "host")
          assert(port == "port")
          assert(endpoint == "")
          assert(channel == "")
        case _ => fail("extractor returns None")
      }
    }

    "be (,,endpoint_1,ch_1) when input `endpoint_1/ch_1`" in {
      "endpoint_1/ch_1" match {
        case RbnbSource(host, port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "endpoint_1")
          assert(channel == "ch_1")
        case _ => fail("extractor returns None")
      }
    }

    "be (,,ep_1,ch_1) when input `/ep_1/ch_1`" in {
      "/ep_1/ch_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "ep_1")
          assert(channel == "ch_1")
        case _ => fail("extractor returns None")
      }
    }
    "be (host,,ep_1,ch_1) when input `host/ep_1/ch_1`" in {
      "host/ep_1/ch_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "host")
          assert(endpoint == "ep_1")
          assert(channel == "ch_1")
          //fail("RbnbHost require both host and port")
        case _ => fail("extractor return None")
      }
    }
    "be (host,port,ep_1) when input `host:port/ep_1`" in {
      "host:port/ep_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "host")
          assert(port == "port")
          assert(endpoint == "ep_1")
          assert(channel == "")
        case _ => fail("extractor returns None")
      }
    }
    "be (,,ep_1,) when input `host:/ep_1`" in {
      "host:/ep_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "ep_1")
          assert(channel == "")
        case _ => fail("invalid return")
      }
    }
    "be (,,ep_1,) when input `:port/ep_1`" in {
      ":port/ep_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "ep_1")
          assert(channel == "")
        case _ => fail("invalid return")
      }
    }
    "be (,,ep_1,) when input `:/ep_1`" in {
      ":/ep_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "")
          assert(port == "")
          assert(endpoint == "ep_1")
          assert(channel == "")
        case _ => fail("invalid return")
      }
    }
    "be (,,,ch_1) when input `://ch_1`" in {
      "://ch_1" match {
        case RbnbSource(host,port, endpoint, channel) => fail("RbnbHost requires host and port")
        case _ => ()
      }
    }
    "be (h,p,ep_1,ch_1) when input `h:p/ep_1/ch_1`" in {
      "h:p/ep_1/ch_1" match {
        case RbnbSource(host,port, endpoint, channel) =>
          assert(host == "h")
          assert(port == "p")
          assert(endpoint == "ep_1")
          assert(channel == "ch_1")
        case _ => fail("extractor returns None")
      }
    }
  }

  "A RbnbSource apply" should {
    "use `localhost` when host is missing" in {
      assert("localhost:1/ep_1/ch_1" == RbnbSource("", "1", "ep_1", "ch_1"))
    }
    "use port 3333 when port is missing" in {
      assert("1:3333/ep_1/ch_1" == RbnbSource("1","","ep_1","ch_1"))
    }
    "produce host:port/endpoint when channel is missing" in {
      assert("host:port/ep_1" == RbnbSource("host","port","ep_1",""))
    }
    "produce empty when endpoint is missing" in {
      assert("host:port" == RbnbSource("host","port", "", "ch_1"))
    }
    "produce localhost:3333/ep_1 when input (,,ep_1,)" in {
      assert("localhost:3333/ep_1" == RbnbSource("","","ep_1",""))
    }
    "produce localhost:3333/ep_1/ch_1 when input (,,ep_1,ch_1)" in {
      assert("localhost:3333/ep_1/ch_1" == RbnbSource("","","ep_1","ch_1"))
    }
  }

  "A RbnbHost" should {
    "be None with input empty" in {
      "" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be None with input `sometext`" in {
      "sometext" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be None with input `:`" in {
      ":" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be None with input `host:`" in {
      "host:" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be None with input `:port`" in {
      ":port" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be None with input `::`" in {
      "::" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be None with input `::1`" in {
      "::1" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
    "be (host,port) with input `host:port`" in {
      "host:port" match {
        case RbnbHost(host, port) =>
          assert(host == "host")
          assert(port == "port")
        case _ => fail()
      }
    }
    "be None with input `host:port:1" in {
      "host:port:1" match {
        case RbnbHost(host, port) => fail("extractor should return None")
        case _ => ()
      }
    }
  }
}
