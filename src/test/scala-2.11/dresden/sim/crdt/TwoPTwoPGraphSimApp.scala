package dresden.sim.crdt

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import dresden.crdt.Ports._
import dresden.crdt.graph.TwoPTwoPGraph
import dresden.crdt.set.ORSet
import dresden.crdt.set.ORSetManager.{AddOperation, RemoveOperation}
import dresden.sim.SimUtil.DresdenTimeout
import dresden.sim.{SimUtil, SimulationResultSingleton}
import se.sics.kompics.Start
import se.sics.kompics.sl.{ComponentDefinition, handle}
import se.sics.kompics.timer.{SchedulePeriodicTimeout, Timer}
import se.sics.ktoolbox.util.network.KAddress

object TwoPTwoPGraphSimApp {

    case class Init(selfAdr: KAddress) extends se.sics.kompics.Init[TwoPTwoPGraphSimApp]

}

class TwoPTwoPGraphSimApp(val init: TwoPTwoPGraphSimApp.Init) extends ComponentDefinition with StrictLogging {

    val mngr = requires[TwoPTwoPGraphManagement]
    val timer = requires[Timer]

    var graph: Option[TwoPTwoPGraph[String]] = None

    val self = init match {
        case TwoPTwoPGraphSimApp.Init(self) => self
    }

    private val period: Long = 1000 // TODO
    private var timerIds: Set[UUID] = Set.empty[UUID]

    private var numSends: Int = 0
    private var maxSends: Int = 20


    ctrl uponEvent {
        case _: Start => handle {
            logger.info(s"$self starting...")
            val spt = new SchedulePeriodicTimeout(0, period)
            val timeout = DresdenTimeout(spt)
            spt.setTimeoutEvent(timeout)
            trigger(spt -> timer)
            timerIds += timeout.getTimeoutId

            // Fetch our set
            trigger(Get(SimUtil.CRDT_SET_KEY) -> mngr)
        }
    }

    timer uponEvent {
        case DresdenTimeout(_) => handle {
            logger.warn("TODO")
        }
    }

    mngr uponEvent {
        case Response(id, crdt: TwoPTwoPGraph[String]) => handle {
            graph = Some(crdt)
            logger.info(s"$self Received $crdt")
            sendAdd()
        }
        case Update(id, crdt: TwoPTwoPGraph[String]) => handle {
            logger.info(s"Received CRDT update for $id")
            graph = Some(crdt)

//            import scala.collection.JavaConverters._
//            SimulationResultSingleton.getInstance().put(self.getId + SimUtil.ORSET_STR, crdt.entries.asJava)
        }
    }

    private def sendAdd(): Unit = {
        if (numSends < maxSends) {
            logger.debug(s"$self Triggering send")

            numSends += 1
        }
    }

    private def removeRandom(): Unit = {
        if (numSends < maxSends) {
        }
    }

    def random[T](s: Set[T]): T = {
        val n = util.Random.nextInt(s.size)
        s.iterator.drop(n).next
    }

}