package ioinformatics.neo4j.rdf.plugin

import java.io.InputStream
import java.net.URLEncoder
import javax.ws.rs.core.{HttpHeaders, MediaType}

import org.apache.commons.io.IOUtils
import org.codehaus.jackson.node.ArrayNode
import org.neo4j.harness.{ServerControls, TestServerBuilders}
import org.neo4j.test.server.HTTP
import org.scalatest.time._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}


/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class RdfPluginTest extends FlatSpec with Matchers with BeforeAndAfter {

  var server: ServerControls = null

  before {
   server = TestServerBuilders.newInProcessBuilder.withExtension("/test", classOf[RdfResource])
      .withExtension("/test", classOf[SparqlResource]).newServer
  }

  after {
    server.close()
  }


  "insertRdf" should
    "store the RDF triples on the Neo4J graph" in {
    val payload: String = testTriples

    // Given
    val response: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN).
      PUT(server.httpURI.resolve("test").toString, HTTP.RawPayload.rawPayload(payload))
    response.status should be(200)

    val query = "select * where { ?s ?p ?o }"
    val queryResponse: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
      .POST(server.httpURI.resolve("test/sparql").toString, HTTP.RawPayload.rawPayload(s"query=${urlEncode(query)}"))

    println(queryResponse)
    queryResponse.status should be(200)
    queryResponse.get("results").get("bindings").asInstanceOf[ArrayNode].size() should be(2)
    val firstBinding = queryResponse.get("results").get("bindings").get(0)
    firstBinding.get("s").get("value").asText() should be("http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1")
    val secondBinding = queryResponse.get("results").get("bindings").get(1)
    secondBinding.get("o").get("type").asText() should be("literal")
    secondBinding.get("o").get("value").asText() should be("Thing")

    val vars = queryResponse.get("head").get("vars").asInstanceOf[ArrayNode]
    vars.get(0).asText() should be("s")
    vars.get(1).asText() should be("p")
    vars.get(2).asText() should be("o")
  }

  it should "insert only one node per URI" in {
    val payload: String = _10000Triples

    // Given
    val response1: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN).
      PUT(server.httpURI.resolve("test").toString, HTTP.RawPayload.rawPayload(payload))
    response1.status should be(200)

    val response2: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN).
      PUT(server.httpURI.resolve("test").toString, HTTP.RawPayload.rawPayload(payload))
    response2.status should be(200)

    val query = "select * where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.ebi.ac.uk/terms/chembl#Activity> }"
    val queryResponse: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
      .POST(server.httpURI.resolve("test/sparql").toString, HTTP.RawPayload.rawPayload(s"query=${urlEncode(query)}"))

    queryResponse.status should be(200)
    queryResponse.get("results").get("bindings").asInstanceOf[ArrayNode].size() should be(19998)
  }


  "executeSparql" should
    "return empty results" in {
    val query = "select * where { ?s ?p ?o }"

    val response: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
      .POST(server.httpURI.resolve("test/sparql").toString, HTTP.RawPayload.rawPayload(s"query=${urlEncode(query)}"))
    response.status should be(200)
    response.get("results").get("bindings").asInstanceOf[ArrayNode].size() should be(0)

  }

  private def _10000Triples: String = {
    val input: InputStream = getClass.getResourceAsStream("/test.nt")
    val buffer: StringBuffer = new StringBuffer
    import scala.collection.JavaConversions._
    for (line <- IOUtils.readLines(input)) {
      buffer.append(line).append("\n")
    }
    buffer.toString
  }

  private def testTriples: String = "<http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/ProductType> .\n"+
    "<http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> <http://www.w3.org/2000/01/rdf-schema#label> \"Thing\" ."

  private def repitedTriple: String = "<http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/ProductType> .\n"+
    "<http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/ProductType> ."


  private def berlin100: String = {
    val input: InputStream = getClass.getResourceAsStream("/berlin_nt_100.nt")
    val buffer: StringBuffer = new StringBuffer
    import scala.collection.JavaConversions._
    for (line <- IOUtils.readLines(input)) {
      buffer.append(line).append("\n")
    }
    buffer.toString
  }

  private def urlEncode(text: String) = URLEncoder.encode(text, "utf8")

}
