package ioinformatics.neo4j.rdf.plugin.writer

import java.io.OutputStream
import javax.ws.rs.core.StreamingOutput

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import info.aduna.iteration.CloseableIteration
import ioinformatics.neo4j.rdf.plugin.writer.SparqlResultsJsonWriter._
import org.openrdf.model.{BNode, Literal, URI, Value}
import org.openrdf.query.{BindingSet, QueryEvaluationException}

import scala.collection.Iterator
import scala.collection.JavaConversions._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class SparqlResultsJsonWriter(result: SparqlResult, vars: Seq[String],
                                 onComplete: => Unit) extends StreamingOutput {

  def onComplete(f: => Unit): SparqlResultsJsonWriter = new SparqlResultsJsonWriter(result, vars, f)

  override def write(output: OutputStream): Unit = {
    val generator = new ObjectMapper().getFactory.createGenerator(output)
    generator.writeStartObject()
    writeHead(generator)
    writeResults(generator)
    generator.writeEndObject()
    generator.close()
    onComplete
  }

  protected def writeHead(generator: JsonGenerator): Unit = {
    generator.writeObjectFieldStart("head")
    generator.writeArrayFieldStart("vars")
    vars.foreach(v => generator.writeString(v))
    generator.writeEndArray()
    generator.writeEndObject()
  }

  protected def writeResults(generator: JsonGenerator): Unit = {
    generator.writeFieldName("results")
    generator.writeStartObject()
    generator.writeArrayFieldStart("bindings")
    for (binding <- result) {
      generator.writeStartObject()
      for (variable <- binding.getBindingNames) {
        generator.writeFieldName(variable)
        writeValue(binding.getBinding(variable).getValue, generator)
      }
      generator.writeEndObject()
    }
    generator.writeEndArray()
    generator.writeEndObject()
  }

  protected def writeValue(value: Value, generator: JsonGenerator): Unit = {
    generator.writeStartObject()
    generator.writeStringField("type", valueType(value))
    generator.writeStringField("value", value.stringValue())
    generator.writeEndObject()
  }

  private def valueType(value: Value): String = value match {
    case _: URI => "uri"
    case _: Literal => "literal"
    case _: BNode => "bnode"
  }
}

object SparqlResultsJsonWriter {
  type SparqlResult = CloseableIteration[_ <: BindingSet, QueryEvaluationException]

  def apply(result: SparqlResult, vars: Seq[String],
            onComplete: => Unit = {}) = new SparqlResultsJsonWriter(result, vars, onComplete)

  implicit def sparqlResultsIterator(result: SparqlResult): Iterator[_ <: BindingSet] = new SparqlResultsIterator(result)
}

private class SparqlResultsIterator(results: SparqlResult) extends Iterator[BindingSet] {
  def hasNext: Boolean = results.hasNext

  def next(): BindingSet = results.next
}