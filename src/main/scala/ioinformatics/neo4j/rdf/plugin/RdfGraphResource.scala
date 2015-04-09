package ioinformatics.neo4j.rdf.plugin

import javax.ws.rs.core.{MediaType, Context, Response}
import javax.ws.rs._

import info.aduna.iteration.CloseableIteration
import ioinformatics.neo4j.rdf.plugin.serialization.SparqlResultJsonOutput
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Transaction
import org.openrdf.query.parser.sparql.SPARQLParser
import org.openrdf.query.{QueryEvaluationException, BindingSet}
import org.openrdf.query.impl.EmptyBindingSet
import org.openrdf.query.parser.ParsedQuery
import org.slf4j.LoggerFactory

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
@Path("/graph")
class RdfGraphResource(@Context neo4j: GraphDatabaseService) {

  private val log = LoggerFactory.getLogger(classOf[RdfGraphResource])

  import ioinformatics.neo4j.rdf.plugin.sail.Neo4jSailConversions._
  import ioinformatics.neo4j.rdf.plugin.serialization.SparqlResultJsonOutput._

  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Path("/sparql")
  def executeSPARQL(@FormParam("query") queryString: String): Response = {
    val connection = neo4j.sailConnection
    val parser = new SPARQLParser
    try {
      val query = parser.parseQuery(queryString, null)
      val sparqlResults: SparqlResultJsonOutput = connection.evaluate(query.getTupleExpr, query.getDataset, new EmptyBindingSet, false)

      Response.ok(sparqlResults).build()
    }
    catch {
      case e: Exception =>
        log.error("Fail to execupte SPARQL", e)
        Response.serverError().entity(e.getMessage).build()
    }
    finally {
      if (connection != null) connection.close()
    }
  }


}
