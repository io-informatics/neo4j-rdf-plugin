package ioinformatics.neo4j.rdf.plugin

import java.io.InputStream
import javax.ws.rs.core.{HttpHeaders, MediaType, Context, Response}
import javax.ws.rs._

import ioinformatics.neo4j.rdf.plugin.serialization.SparqlResultJsonOutput
import ioinformatics.neo4j.rdf.plugin.util.LoadMonitor
import org.neo4j.graphdb.GraphDatabaseService
import org.openrdf.model.ValueFactory
import org.openrdf.query.parser.sparql.SPARQLParser
import org.openrdf.query.impl.EmptyBindingSet
import org.openrdf.repository.util.{RDFLoader, RDFInserter}
import org.openrdf.rio.{RDFFormat, RDFHandler}
import org.openrdf.rio.helpers.RDFHandlerWrapper
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
@Path("/")
class RdfGraphResource(@Context neo4j: GraphDatabaseService) {

  private val log = LoggerFactory.getLogger(classOf[RdfGraphResource])

  import ioinformatics.neo4j.rdf.plugin.sail.Neo4jSailConversions._
  import ioinformatics.neo4j.rdf.plugin.serialization.SparqlResultJsonOutput._

  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Path("/sparql")
  def executeSPARQL(@FormParam("query") queryString: String): Response = {
    log.debug(s"Executing SPARQL query: $queryString")
    val connection = neo4j.sailConnection
    val parser = new SPARQLParser
    try {
      val query = parser.parseQuery(queryString, null)
      val vars =  query.getTupleExpr.getBindingNames.asScala.toSeq
      val sparqlResults = SparqlResultJsonOutput(connection.evaluate(query.getTupleExpr, query.getDataset, new EmptyBindingSet, false), vars) onComplete {
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

  @PUT
  @Produces(Array(MediaType.APPLICATION_JSON))
  def insertRdf(@HeaderParam(HttpHeaders.CONTENT_TYPE) contentType: String,
                @QueryParam("context") context: String,
                rdfStream: InputStream): Response = {

    //TODO: Improve!

    log.debug("Loading RDF")
    val connection = neo4j.repositoryConnection
    val vf: ValueFactory = connection.getValueFactory
    try {
      val rdfInserter: RDFInserter = new RDFInserter(connection)
      if (context != null) {
        rdfInserter.enforceContext(vf.createURI(context))
      }
      val loader: RDFLoader = new RDFLoader(connection.getParserConfig, vf)
      val baseUri: String = if (context == null) "" else context
      val aggregatedHandlers: RDFHandler = new RDFHandlerWrapper(rdfInserter, LoadMonitor.withLogging())
      connection.begin()
      loader.load(rdfStream, baseUri, RDFFormat.forMIMEType(contentType, RDFFormat.NTRIPLES), aggregatedHandlers)
      connection.commit()
      Response.ok.build
    }
    catch {
      case e: Exception =>
        log.error("Failed RDF load", e)
        connection.rollback()
        Response.serverError().entity(e.getMessage).build()
    }
    finally {
      log.debug("Ended data loading for context {}", context)
      if (connection != null) connection.close()
    }
  }




}
