package ioinformatics.neo4j.rdf.plugin.wrapper

import com.tinkerpop.blueprints.KeyIndexableGraph
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph
import com.tinkerpop.blueprints.oupls.sail.GraphSail
import ioinformatics.neo4j.rdf.plugin.optimization.CachingGraphSail
import org.neo4j.graphdb.GraphDatabaseService
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.NotifyingSailConnection

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
object Neo4jSailWrapper {

  implicit def GraphSailDatabaseService(db: GraphDatabaseService) = new {

    lazy val graphSail: GraphSail[KeyIndexableGraph] = {
      val neo4jGraph = new Neo4j2Graph(db)
      val graphSail = new GraphSail[KeyIndexableGraph](neo4jGraph) with CachingGraphSail[KeyIndexableGraph]
      graphSail.initialize
      graphSail
    }

    def sailConnection: NotifyingSailConnection = graphSail.getConnection

    def sailRepositoryConnection: RepositoryConnection = new SailRepository(graphSail).getConnection
  }

}
