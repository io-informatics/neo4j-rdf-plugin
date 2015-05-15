package ioinformatics.neo4j.rdf.plugin

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.ws.rs._
import javax.ws.rs.core.{Context, MediaType, Response}

import ioinformatics.neo4j.rdf.plugin.writer.SparqlResultsJsonWriter
import org.apache.commons.io.IOUtils
import org.neo4j.graphdb.GraphDatabaseService
import org.openrdf.query.impl.EmptyBindingSet
import org.openrdf.query.parser.sparql.SPARQLParser
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * @author Alberto J. Rubio <yebenes9@gmail.com>
 */
@Path("/sparql")
class SparqlResource(@Context neo4j: GraphDatabaseService) {

  private val log = LoggerFactory.getLogger(classOf[SparqlResource])

  import ioinformatics.neo4j.rdf.plugin.wrapper.Neo4jSailWrapper._

  lazy val html: String = loadHtml()

  @GET
  @Produces(Array(MediaType.TEXT_HTML))
  def getUI(): Response = {
    Response.ok(html).`type`(MediaType.TEXT_HTML).build()
  }

  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  def executeSPARQL(@FormParam("query") queryParam: String): Response = {
    log.debug(s"Executing SPARQL query: $queryParam")
    val connection = neo4j.sailConnection
    val parser = new SPARQLParser
    try {
      val query = parser.parseQuery(queryParam, null)
      val vars = query.getTupleExpr.getBindingNames.asScala.toSeq
      val sparqlResults = SparqlResultsJsonWriter(connection.evaluate(query.getTupleExpr,
        query.getDataset, new EmptyBindingSet, false), vars) onComplete {
        if (connection != null) connection.close()
      }
      Response.ok(sparqlResults).build()
    }
    catch {
      case e: Exception =>
        log.error("Failed to execute SPARQL", e)
        if (connection != null) connection.close()
        Response.serverError().entity(e.getMessage).build()
    }
  }

  private def loadHtml(): String = {
    val in = this.getClass.getResourceAsStream("/web/sparql.html")
    IOUtils.toString(in, Charset.forName("utf-8"))
  }
}
