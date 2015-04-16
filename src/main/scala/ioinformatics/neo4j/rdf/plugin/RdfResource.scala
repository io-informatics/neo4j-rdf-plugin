package ioinformatics.neo4j.rdf.plugin

import java.io.InputStream
import javax.ws.rs._
import javax.ws.rs.core.{Context, HttpHeaders, MediaType, Response}

import ioinformatics.neo4j.rdf.plugin.util.LoadMonitor
import org.neo4j.graphdb.GraphDatabaseService
import org.openrdf.model.ValueFactory
import org.openrdf.repository.util.{RDFInserter, RDFLoader}
import org.openrdf.rio.helpers.RDFHandlerWrapper
import org.openrdf.rio.{RDFFormat, RDFHandler}
import org.slf4j.LoggerFactory

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */

@Path("/rdf")
class RdfResource(@Context neo4j: GraphDatabaseService) {

  private val log = LoggerFactory.getLogger(classOf[RdfResource])

  import ioinformatics.neo4j.rdf.plugin.wrapper.Neo4jSailWrapper._

  @PUT
  @Produces(Array(MediaType.APPLICATION_JSON))
  def insertRdf(@HeaderParam(HttpHeaders.CONTENT_TYPE) contentType: String,
                @QueryParam("context") context: String, rdfStream: InputStream): Response = {

    //TODO: Improve!
    log.debug("Loading RDF")
    val repositoryConnection = neo4j.sailRepositoryConnection
    val valueFactory: ValueFactory = repositoryConnection.getValueFactory
    try {
      val rdfInserter: RDFInserter = new RDFInserter(repositoryConnection)
      if (context != null) {
        rdfInserter.enforceContext(valueFactory.createURI(context))
      }
      val loader: RDFLoader = new RDFLoader(repositoryConnection.getParserConfig, valueFactory)
      val baseUri: String = if (context == null) "" else context
      val aggregatedHandlers: RDFHandler = new RDFHandlerWrapper(rdfInserter, LoadMonitor.withLogging())
      repositoryConnection.begin()
      loader.load(rdfStream, baseUri, RDFFormat.forMIMEType(contentType, RDFFormat.NTRIPLES), aggregatedHandlers)
      repositoryConnection.commit()
      Response.ok.build
    }
    catch {
      case e: Exception =>
        log.error("Failed RDF load", e)
        repositoryConnection.rollback()
        Response.serverError().entity(e.getMessage).build()
    }
    finally {
      log.debug("Ended data loading for context {}", context)
      if (repositoryConnection != null) repositoryConnection.close()
    }
  }
}
