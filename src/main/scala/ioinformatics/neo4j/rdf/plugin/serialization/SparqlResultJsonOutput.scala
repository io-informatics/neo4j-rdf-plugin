package ioinformatics.neo4j.rdf.plugin.serialization

import java.io.OutputStream
import javax.ws.rs.core.StreamingOutput

import com.fasterxml.jackson.core.{JsonGenerator, JsonFactory}
import com.fasterxml.jackson.databind.ObjectMapper
import info.aduna.iteration.{Iteration, CloseableIteration}
import org.openrdf.model.{BNode, Literal, URI, Value}
import org.openrdf.query.{QueryEvaluationException, BindingSet}
import SparqlResultJsonOutput._
import scala.collection.Iterator
import scala.collection.JavaConversions._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class SparqlResultJsonOutput(result: SparqlResult, vars: Seq[String], onComplete: => Unit) extends StreamingOutput {
  override def write(output: OutputStream): Unit = {
    val generator = jsonFactory.createGenerator(output)
    generator.writeStartObject()

    writeHead(generator)
    writeResults(generator)

    generator.writeEndObject()
    generator.close()
    onComplete
  }

  def onComplete(f: => Unit): SparqlResultJsonOutput = new SparqlResultJsonOutput(result, vars, f)

  protected def writeValue(value: Value, generator: JsonGenerator): Unit = {
    generator.writeStartObject()
    generator.writeStringField("type", valueType(value))
    generator.writeStringField("value", value.stringValue())
    generator.writeEndObject()
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

    for(binding <- result) {
      generator.writeStartObject()
      for(variable <- binding.getBindingNames){
        generator.writeFieldName(variable)
        writeValue(binding.getBinding(variable).getValue, generator)
      }
      generator.writeEndObject()
    }

    generator.writeEndArray()
    generator.writeEndObject()
  }

  private def valueType(value: Value): String = value match {
    case _: URI => "uri"
    case _: Literal => "literal"
    case _: BNode => "bnode"
  }
}

object SparqlResultJsonOutput {
  type SparqlResult = CloseableIteration[_ <: BindingSet, QueryEvaluationException]

  def apply(result: SparqlResult, vars: Seq[String], onComplete: => Unit = {}) = new SparqlResultJsonOutput(result, vars, onComplete)

  implicit def sparqlResultToIterator(result: SparqlResult): Iterator[_ <: BindingSet] = new IterationWrapper(result)

  private val jsonFactory = new ObjectMapper().getFactory
}

private class IterationWrapper(underlying: SparqlResult)  extends Iterator[BindingSet]  {
  def hasNext : Boolean = underlying.hasNext

  def next() : BindingSet = underlying.next()
}