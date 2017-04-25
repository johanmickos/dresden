package dresden.sim

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import dresden.components.Ports.{GBEB_Broadcast, GBEB_Deliver, GossippingBestEffortBroadcast}
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timeout, Timer}
import se.sics.kompics.{KompicsEvent, Start}
import se.sics.ktoolbox.util.network.KAddress
import template.kth.app.sim.SimulationResultSingleton

class GossipSimApp(init: Init[GossipSimApp]) extends ComponentDefinition with StrictLogging {
    val timer = requires[Timer]
    val network = requires[Network]
    val gossip = requires[GossippingBestEffortBroadcast]

    private var timerId: Option[UUID] = None
    private val period: Long = 2000 // TODO

    private val self = init match {
        case Init(s: KAddress) => s
    }
    private var sent = Set.empty[String]
    private var received = Set.empty[String]

    ctrl uponEvent {
        case _: Start => handle {
            logger.info("Starting Dresden application")
            val spt = new SchedulePeriodicTimeout(0, period)
            val timeout = DresdenTimeout(spt)
            spt.setTimeoutEvent(timeout)
            trigger(spt -> timer)
            timerId = Some(timeout.getTimeoutId)
        }
    }

    timer uponEvent {
        case DresdenTimeout(_) => handle {
            val id: String = UUID.randomUUID().toString
            logger.info(s"$self triggering gossip $id")
            val payload = GossipPayload(self, UUID.randomUUID().toString)
            trigger(GBEB_Broadcast(payload) -> gossip)
            sent += id

            import scala.collection.JavaConverters._
            SimulationResultSingleton.getInstance().put(self.toString + "-sent", sent.asJava)
            killTimer()
        }
    }

    gossip uponEvent {
        case GBEB_Deliver(_, payload@GossipPayload(from, id)) => handle {
            if (received.contains(id)) {
                logger.warn(s"Duplicated GBEB Deliver message $payload")
            } else {
                received += id
                logger.info(s"$self received gossip $id")

                import scala.collection.JavaConverters._
                SimulationResultSingleton.getInstance().put(self.toString + "-recvd", received.asJava)
            }
        }
        case anything => handle {
            logger.warn(s"$self unexpected gossip event: $anything")
        }
    }

    private def killTimer() = {
        timerId match {
            case Some(id) =>
                trigger(new CancelPeriodicTimeout(id) -> timer)
            case None => // Nothing to do
        }
    }

    override def tearDown(): Unit = {
        killTimer()
    }
}

case class DresdenTimeout(spt: SchedulePeriodicTimeout) extends Timeout(spt)
case class GossipPayload(from: KAddress, id: String) extends KompicsEvent {
    override def equals(o: Any) = o match {
        case that: GossipPayload => that.id.equalsIgnoreCase(this.id)
        case _ => false
    }
    override def hashCode = id.toUpperCase.hashCode
}