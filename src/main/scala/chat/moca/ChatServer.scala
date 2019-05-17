package chat.moca

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.io.StdIn
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.io.Tcp.Message
import akka.util.ByteString

import scala.collection.mutable.Queue


object ChatServer extends App {

  implicit val actorSystem = ActorSystem("chatsystem")
  implicit val materializer = ActorMaterializer()

  val host = "localhost"
  val port = 5000

  private val returnQ = new Queue[Array[ByteString]]

  def handleWebSocketMessages(handler: Flow[Message, Message, Any]): Route ={
    println("in handleWebSockets!!")
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage (Source.single("Hello") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm : BinaryMessage =>
//        returnQ.enqueue(bm.getStrictData)  //ToDo convert to byte array
        //TODO push to queue
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }
    ???
  }

  val echoService: Flow[Message, Message, _] = Flow[Message].map {
//    case TextMessage.Strict(txt) => TextMessage("Echo: " + txt)
    case _ => TextMessage("Message type unsupported")
    ???
  }


  val route = get {
    pathEndOrSingleSlash {
      complete("Welcome to websocket server")
    }
  } ~
    path("ws-echo") {
      get {
        handleWebSocketMessages(echoService)
      }
  } ~
  pathPrefix("ws-chat" / IntNumber) { chatId =>
    parameter('name) { userName =>
      handleWebSocketMessages(ChatRooms.findOrCreate(chatId).websocketFlow(userName))
    }
  }



  val binding = Http().bindAndHandle(route, host, port)
  println(s"Server is now online at http://$host:$port\n Press Return to stop...")
  StdIn.readLine()

//  import actorSystem.dispatcher

  binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
  println("Server is down...")

}
