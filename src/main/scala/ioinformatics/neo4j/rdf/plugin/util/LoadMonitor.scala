package ioinformatics.neo4j.rdf.plugin.util

import java.util.concurrent.atomic.AtomicLong

import org.openrdf.model.Statement
import org.openrdf.rio.helpers.RDFHandlerBase
import org.slf4j.LoggerFactory

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class LoadMonitor(listener: Double=>Unit) extends RDFHandlerBase {

  var counter : AtomicLong = new AtomicLong(0)
  var startTime : Long = 0
  var lastCheck : Long = 0
  var throughput : Double = 0

  override def startRDF(): Unit = {
    startTime  = System.currentTimeMillis()
    lastCheck = startTime
  }

  override def handleStatement(st: Statement): Unit = {
    counter.incrementAndGet()
    val t = System.currentTimeMillis()
    // measure throughput every 2sec
    if (t - lastCheck > 2000) {
      record (counter.get() / ((t - startTime) / 1000))
      lastCheck = t
    }
  }

  private def record(value: Double) = {
    throughput = value
    listener(value)
  }
}

object LoadMonitor {
  private val log = LoggerFactory.getLogger(classOf[LoadMonitor])

  def apply(listener: Double=>Unit = (d)=>{}) = new LoadMonitor(listener)
  def withLogging() = new LoadMonitor (value => {
    log.info(s"Loading data at $value stmts/sec")
  })
}
