package ioinformatics.neo4j.rdf.plugin

import java.net.URLEncoder
import javax.ws.rs.core.{MediaType, HttpHeaders}

import org.codehaus.jackson.node.ArrayNode
import org.neo4j.harness.{TestServerBuilders, ServerControls}
import org.neo4j.test.server.HTTP
import org.scalatest.{Matchers, FlatSpec}

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class RdfGraphResourceTest extends FlatSpec with Matchers {

  "executeSparql" should
    "return empty results" in {
    val query = "select * where { ?s ?p ?o }"
      try {
        val server: ServerControls = TestServerBuilders.newInProcessBuilder.withExtension("/rdf", classOf[RdfGraphResource]).newServer
        try {
          val response: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
            .POST(server.httpURI.resolve("rdf/graph/sparql").toString, HTTP.RawPayload.rawPayload(s"query=${URLEncoder.encode(query)}"))
          response.status should be (200)
          response.get("results").get("bindings").asInstanceOf[ArrayNode].size() should be (0)
        } finally {
          if (server != null) server.close()
        }
      }
    } 

}
