package ioinformatics.neo4j.rdf.plugin.optimization

import org.openrdf.model.Statement
import org.openrdf.repository.RepositoryConnection
import org.openrdf.rio.RDFHandler

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
trait TransactionSplitter extends RDFHandler {

  val connection: RepositoryConnection
  val bufferSize = 10000

  @volatile private var transactionCount = 0

  abstract override def startRDF(): Unit = {
    connection.begin()
    super.startRDF()
  }

  abstract override def handleStatement(st: Statement): Unit = {
    if(transactionCount >= bufferSize) {
      intermediateCommit()
      transactionCount = 0
    }
    super.handleStatement(st)
    transactionCount += 1
  }


  abstract override def endRDF(): Unit = {
    super.endRDF()
    connection.commit()
  }

  private def intermediateCommit(): Unit = {
    connection.commit()
    connection.begin()
  }
}
