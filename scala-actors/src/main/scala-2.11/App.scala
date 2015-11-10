import java.net.ConnectException
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import dispatch._, Defaults._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import com.typesafe.config.ConfigFactory

object App {

  def main(args: Array[String]): Unit = {
    val foo = Stopwatch.start // Forcing initialization
    val actorSystemConf = ConfigFactory.parseString("""
      akka {
        stdout-loglevel = "OFF"
        loglevel = "OFF"
      }
                                                    """)
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val config = Config("localhost:8080", mapper, 1000)
    val system = ActorSystem("MySystem", ConfigFactory.load(actorSystemConf))
    val coordinator = system.actorOf(Props(classOf[AssemblingCoordinator], config), name = "Coordinator")
    coordinator ! config
  }

  class AssemblingCoordinator(config: Config) extends Actor {

    def receive = {
      case config: Config =>
        val req = url(s"http://${config.host}/api/graph/edges-quanty")
        val futureQuanty = Http(req OK as.String) map { _.toInt }
        val quantyEdges = futureQuanty()
        val batches = quantyEdges / config.batchSize + (math signum (quantyEdges % config.batchSize))
        context.become(collectEdges(Array[Edge](), Set[Int]()))
        context.actorOf(Props[EdgesBathProcessor], name = "EdgesBathProcessor") ! (batches, config)
    }

    def collectEdges(edges: Array[Edge], vertices: Set[Int]): Receive = {
      case adjacencies: Array[Edge] =>
        context.become(collectEdges(
          edges ++ adjacencies,
          vertices ++ edges.flatMap(e => (e.i, e.j).productIterator map (_.asInstanceOf[Int]))))
      case _: BatchProcessingFinished =>
        println(s"Vertices: ${vertices.size}")
        println(s"Edges: ${edges.length}")
        println(s"Milliseconds taken: ${Stopwatch elapsedTime}")
        context.system.shutdown()
    }

  }

  class EdgesBathProcessor extends Actor {
    import akka.actor.OneForOneStrategy
    import akka.actor.SupervisorStrategy._

    override val supervisorStrategy = OneForOneStrategy() {
      case _: AdjacencyServerFailure => Restart
      case _: Exception              => Escalate
    }

    def receive = {
      case (batches: Int, config: Config) =>
        context.become(receiveBatchResults(batches, 1))
        for (batch <- List.range(0, batches)) {
          val adjancencyFetcher = context.actorOf(Props[AdjacencyArrayFetcher], name = "AdjacencyArrayFetcher" + batch)
          adjancencyFetcher ! (config, batch * config.batchSize)
        }
    }

    def receiveBatchResults(totalBatches: Int, batchesAlreadyProcessed: Int): Receive = {
      case edges: Array[Edge] =>
        context.parent ! edges
        if (batchesAlreadyProcessed == totalBatches) {
          context.parent ! BatchProcessingFinished()
        } else {
          context.become(receiveBatchResults(totalBatches, batchesAlreadyProcessed + 1))
        }
    }
  }

  class AdjacencyArrayFetcher extends Actor {
    def receive = {
      case (config: Config, offset: Int) =>
        val req = url(s"http://${config.host}/api/graph?offset=$offset&limit=${config.batchSize}")
        val response: Future[Either[Throwable, String]] = Http(req OK as.String).either
        response() match {
          case Left(StatusCode(502))     => throw AdjacencyServerFailure(offset)
          case Left(_: ConnectException) => throw AdjacencyServerFailure(offset)
          case Left(ex)                  => throw ex
          case Right(json)               =>
            context.parent ! config.mapper.readValue(json, new TypeReference[Array[Edge]] {})
            context.stop(self)
        }
    }

    override def preRestart(cause: Throwable, message: Option[Any]): Unit = {
      cause match {
        case AdjacencyServerFailure(_) =>
          message match {
            case Some(m) => self ! m
          }
      }
    }
  }

  case class Config(host: String, mapper: ObjectMapper, batchSize: Int)
  case class Edge(i: Int, j: Int, weight: Int)
  case class AdjacencyServerFailure(offset: Int)
    extends Exception(s"Temporary server failure fetching adjancencies after $offset")
  case class BatchProcessingFinished()
  case object Stopwatch {
    val start: Long = System.nanoTime()

    def elapsedTime(): Long = {
      val stop = System.nanoTime()
      return math.round((stop - start).asInstanceOf[Double] / 1000000)
    }
  }
}
