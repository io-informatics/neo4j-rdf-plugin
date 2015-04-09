package ioinformatics.neo4j.rdf.plugin.serialization

import java.io.OutputStream
import javax.ws.rs.core.StreamingOutput

import com.fasterxml.jackson.core.JsonFactory
import info.aduna.iteration.{Iteration, CloseableIteration}
import org.openrdf.query.{QueryEvaluationException, BindingSet}
import SparqlResultJsonOutput._
import scala.collection.Iterator
import scala.collection.JavaConversions._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class SparqlResultJsonOutput(result: SparqlResult) extends StreamingOutput {
  override def write(output: OutputStream): Unit = {
    val generator = jsonFactory.createGenerator(output)
    generator.writeStartObject()


    generator.writeFieldName("results")
    generator.writeStartObject()
    generator.writeArrayFieldStart("bindings")

    for(binding <- result) {
      generator.writeStartObject()
      for(variable <- binding.getBindingNames){
        generator.writeObjectField(variable, binding.getBinding(variable).getValue)
      }
      generator.writeEndObject()
    }

    generator.writeEndArray()
    generator.writeEndObject()

    generator.writeEndObject()
    generator.close()
  }
}

object SparqlResultJsonOutput {
  type SparqlResult = CloseableIteration[_ <: BindingSet, QueryEvaluationException]

  def apply(result: SparqlResult) = new SparqlResultJsonOutput(result)

  implicit def sparqlResultToJsonOutput(result: SparqlResult): SparqlResultJsonOutput = apply(result)
  implicit def sparqlResultToIterator(result: SparqlResult): Iterator[_ <: BindingSet] = new IterationWrapper(result)

  private val jsonFactory = new JsonFactory()
}

private class IterationWrapper(underlying: SparqlResult)  extends Iterator[BindingSet]  {
  def hasNext : Boolean = underlying.hasNext

  def next() : BindingSet = underlying.next()
}