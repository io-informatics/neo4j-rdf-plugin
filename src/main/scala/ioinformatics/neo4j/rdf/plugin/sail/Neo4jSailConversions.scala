package ioinformatics.neo4j.rdf.plugin.sail

import com.tinkerpop.blueprints.KeyIndexableGraph
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph
import com.tinkerpop.blueprints.oupls.sail.GraphSail
import org.neo4j.graphdb.GraphDatabaseService
import org.openrdf.sail.NotifyingSailConnection

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
object Neo4jSailConversions {

  implicit def wrappGraphDatabaseService(db: GraphDatabaseService) = new {

    lazy val sail: GraphSail[KeyIndexableGraph] = {
      val neo4jGraph = new Neo4j2Graph(db)
      val sail = new GraphSail[KeyIndexableGraph](neo4jGraph)
      sail.initialize
      sail
    }

    def sailConnection: NotifyingSailConnection = sail.getConnection
  }

}
