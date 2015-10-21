import akka.pattern.pipe
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import dispatch._, Defaults._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor

object App {
  def main(args: Array[String]): Unit = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val config = Config("localhost:8080", mapper, 1000)
    val system = ActorSystem("MySystem")
    val assembler = system.actorOf(Props(classOf[GraphAssembler], config), name = "GraphAssembler")
    Thread sleep 3000
    assembler ! "stop"
  }

  class GraphAssembler(config: Config) extends Actor {
    context.actorOf(Props[EdgesFetcher], name = "EdgesFetcher") ! config.host

    override def receive = {
      case quantyEdges: Int =>
        val batches = quantyEdges / config.batchSize + (if (quantyEdges % config.batchSize > 0) 1 else 0)
        for (batch <- List.range(0, 1)) {
          context.actorOf(Props[AdjacencyArrayFetcher], name = "AdjacencyArrayFetcher" + batch) ! (config, batch * config.batchSize)
        }
      case adjacencies: Either[Throwable, Array[Edge]] => adjacencies match {
        case Left(ex) =>
          ex.printStackTrace()
        case Right(edges) =>
          println("becoming")
          context.become(receiveEdgeBatch(edges))
      }
      case f: akka.actor.Status.Failure => f.cause.printStackTrace()
      case x => println(x.getClass)
    }

    def receiveEdgeBatch(edges: Array[Edge]): Receive = {
      case adjacencies: Array[_] =>
        adjacencies.head match {
          case e: Edge =>
            println(edges.size)
            context.become(receiveEdgeBatch(edges ++ adjacencies.asInstanceOf[Array[Edge]]))
        }
      case "stop" =>
        println(edges.mkString(", "))
        context.system.shutdown()
    }

  }

  class EdgesFetcher extends Actor {
    override def receive = {
      case host: String =>
        val req = url(s"http://$host/api/graph/edges-quanty")
        val response = Http(req OK as.String)
        val quanty = response map { _.toInt }
        quanty pipeTo sender
    }
  }

  class AdjacencyArrayFetcher extends Actor {
    override def receive = {
      case (config: Config, offset: Int) =>
        val req = url(s"http://${config.host}/api/graph?offset=$offset&limit=${config.batchSize}")
        val response = Http(req OK as.String).either
        val adjancencyArray: Future[Either[Throwable, List[Edge]]] = response map {
          case Left(ex) => Left(ex)
          case Right(json) => Right(config.mapper.readValue(json, new TypeReference[Array[Edge]]{}))
        }
        adjancencyArray pipeTo sender
    }
  }

  case class Config(host: String, mapper: ObjectMapper, batchSize: Int)
  case class Edge(i: Int, j: Int, weight: Int)
}
