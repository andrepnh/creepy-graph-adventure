import akka.pattern.pipe
import dispatch._, Defaults._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor

object App {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("MySystem")
    val assembler = system.actorOf(Props[GraphAssembler], name = "GraphAssembler")
    assembler ! Config("localhost:8080")
  }

  class GraphAssembler extends Actor {
    override def receive = {
      case c: Config =>
        context.actorOf(Props[EdgesFetcher], name = "EdgesFetcher") ! c.host
      case q: Int =>
        println(q)
        context.stop(self)
        context.system.shutdown()
      case x =>
        println("unknown message 1 " + x)
        context.system.shutdown()
    }

    override def postStop = println("GraphAssembler - stopped")

  }

  class EdgesFetcher extends Actor {
    override def receive = {
      case host: String =>
        val req = url(s"http://$host/api/graph/edges-quanty")
        val response = Http(req OK as.String)
        val quanty = response map { _.toInt }
        quanty pipeTo sender
        context.stop(self)
      case _ => println("unknown message 2")
    }
  }

  case class Config(host: String)
}
