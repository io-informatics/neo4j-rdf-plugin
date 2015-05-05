package ioinformatics.neo4j.rdf.plugin.optimization

import com.tinkerpop.blueprints.oupls.sail.GraphSail
import com.tinkerpop.blueprints.oupls.sail.GraphSail.DataStore
import com.tinkerpop.blueprints.{KeyIndexableGraph, Vertex}
import org.cache2k.{CacheBuilder, CacheSource}
import org.openrdf.model.{Statement, Value}
import CachingGraphSail._
import org.openrdf.sail._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
trait CachingGraphSail[T <: KeyIndexableGraph] extends GraphSail[T] {

  override def createStore(): DataStore[T] = new DataStore[T] with CachedDataStore[T]

  trait CachedDataStore[T <: KeyIndexableGraph] extends  DataStore[T] {

    val vertexCache = CacheBuilder.newCache(classOf[Value], classOf[Object]).
      name("CachingGraphSail").
      maxSize(1000000).
      eternal(true).
      source((getVertexId _).andThen(_.orNull)).
      build()

    abstract override def getVertex(value: Value): Vertex = getVertexById(vertexCache.get(value))
    private def getVertexId(value: Value): Option[Object] = Option(super.getVertex(value)).map(_.getId)
  }

  private def getVertexById(id: Object): Vertex = if(id == null) null else getBaseGraph.getVertex(id)

}

object CachingGraphSail {
  implicit  def FunctionAsCacheSource[A,B](f: A=>B): CacheSource[A, B] = new CacheSource[A,B] {
    override def get(k: A): B = f(k)
  }
}