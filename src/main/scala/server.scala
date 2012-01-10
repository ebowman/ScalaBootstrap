package com.gilt.hackathon

import akka.config.Supervision._
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

import cc.spray.can._
import HttpMethods.GET
import akka.actor._

object Main extends App {
  Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      List(
        Supervise(Actor.actorOf(new StreamActor("stream-endpoint")), Permanent),
        Supervise(Actor.actorOf(new HttpServer()), Permanent)
      )
    )
  )
}

class StreamActor(id: String) extends Actor {
  val log = LoggerFactory.getLogger(getClass)
  self.id = id

  def receive = {

    case RequestContext(HttpRequest(GET, "/", _, _, _), _, responder) =>
      responder.complete(index)

    case RequestContext(HttpRequest(GET, "/stream", _, _, _), _, responder) =>
      val streamHandler = responder.startChunkedResponse(HttpResponse(headers = defaultHeaders))
      val chunkGenerator = Scheduler.schedule(
        () => streamHandler.sendChunk(MessageChunk(DateTime.now.toIsoDateTimeString + ", ")),
        0, 100, TimeUnit.MILLISECONDS
      )
      Scheduler.scheduleOnce(() => {
        chunkGenerator.cancel(false)
        streamHandler.sendChunk(MessageChunk("\nStopped..."))
        streamHandler.close()
      }, 5000, TimeUnit.MILLISECONDS)

    case RequestContext(HttpRequest(_, path, _, _, _), _, responder) =>
      responder.complete(response("Unknown resource", 404))

    case Timeout(method, uri, _, _, _, complete) =>
      complete {
        HttpResponse(status = 500).withBody("The " + method + " request to '" + uri + "' has timed out...")
      }
  }

  private val defaultHeaders = List(HttpHeader("Content-Type", "text/plain"))

  private def response(msg: String, status: Int = 200) = HttpResponse(status, defaultHeaders, msg.getBytes("ISO-8859-1"))

  private lazy val index = HttpResponse(
    headers = List(HttpHeader("Content-Type", "text/html")),
    body =
      <html>
        <body>
          <ul>
            <li>
              <a href="/stream">/stream</a>
            </li>
          </ul>
        </body>
      </html>.toString().getBytes("ISO-8859-1")
  )
}
