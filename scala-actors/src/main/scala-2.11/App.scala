import java.io.StringWriter

import akka.pattern.pipe
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import dispatch._, Defaults._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import scala.reflect.runtime.universe._

object App {
  def main(args: Array[String]): Unit = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val config = Config("localhost:8080", mapper, 1000)
    val system = ActorSystem("MySystem")
    val assembler = system.actorOf(Props(classOf[GraphAssembler], config), name = "GraphAssembler")
  }

  class GraphAssembler(config: Config) extends Actor {
    context.actorOf(Props[EdgesFetcher], name = "EdgesFetcher") ! config.host

    override def receive = {
      case quantyEdges: Int =>
        val batches = quantyEdges / config.batchSize + (if (quantyEdges % config.batchSize > 0) 1 else 0)
        for (batch <- List.range(0, batches)) {
          context.actorOf(Props[AdjacencyArrayFetcher], name = "AdjacencyArrayFetcher" + batch) ! (config, batch * config.batchSize)
        }
      case adjacencies: List[_] => adjacencies.head match {
        case e: Edge =>
          println("becoming")
          context.become(receiveEdgeBatch(adjacencies.asInstanceOf[List[Edge]]))
      }
      case x => println(x.getClass)
    }

    def receiveEdgeBatch(edges: List[Edge]): Receive = {
      case adjacencies: List[_] => adjacencies.head match {
        case e: Edge =>
          println(edges.size)
          context.become(receiveEdgeBatch(edges ++ adjacencies.asInstanceOf[List[Edge]]))
      }
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
        val req = url(s"http://$host/api/graph?offset=$offset&limit=${config.batchSize}")
        val response = Http(req OK as.String)
        val edgeArrayType = config.mapper.getTypeFactory().constructArrayType(List.getClass)
        val adjancencyArray = response map {json =>
          try {
            config.mapper.readValue(json, edgeArrayType)
          } catch {
            case e => e.printStackTrace()
          }
        }
        println("read json")
        adjancencyArray pipeTo sender
    }
  }

  case class Config(host: String, mapper: ObjectMapper, batchSize: Int)
  case class Edge(i: Int, j: Int, weigth: Int)
}
