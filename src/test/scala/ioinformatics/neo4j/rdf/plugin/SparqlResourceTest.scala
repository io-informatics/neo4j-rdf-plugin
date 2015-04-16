package ioinformatics.neo4j.rdf.plugin

import java.net.URLEncoder
import javax.ws.rs.core.{HttpHeaders, MediaType}

import org.codehaus.jackson.node.ArrayNode
import org.neo4j.harness.{ServerControls, TestServerBuilders}
import org.neo4j.test.server.HTTP
import org.scalatest.{FlatSpec, Matchers}

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class SparqlResourceTest extends FlatSpec with Matchers {

  "executeSparql" should
    "return empty results" in {
    val query = "select * where { ?s ?p ?o }"
    try {
      val server: ServerControls = TestServerBuilders.newInProcessBuilder.withExtension("/test", classOf[SparqlResource]).newServer
      try {
        val response: HTTP.Response = HTTP.withHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
          .POST(server.httpURI.resolve("test/sparql").toString, HTTP.RawPayload.rawPayload(s"query=${urlEncode(query)}"))
        response.status should be(200)
        response.get("results").get("bindings").asInstanceOf[ArrayNode].size() should be(0)
      } finally {
        if (server != null) server.close()
      }
    }
  }

  private def urlEncode(text: String) = URLEncoder.encode(text, "utf8")
}
