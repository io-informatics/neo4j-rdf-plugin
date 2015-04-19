package ioinformatics.neo4j.rdf.plugin.optimization

import com.tinkerpop.blueprints.oupls.sail.GraphSail
import com.tinkerpop.blueprints.oupls.sail.GraphSail.DataStore
import com.tinkerpop.blueprints.{KeyIndexableGraph, Vertex}
import org.cache2k.{CacheBuilder, CacheSource}
import org.openrdf.model.Value
import CachingGraphSail._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
trait CachingGraphSail[T <: KeyIndexableGraph] extends GraphSail[T] {

  override def createStore(): DataStore[T] = new DataStore[T] with CachedDataStore[T]

  trait CachedDataStore[T <: KeyIndexableGraph] extends  DataStore[T] {

    val vertexCache = CacheBuilder.newCache(classOf[Value], classOf[Vertex]).
      maxSize(100000).
      eternal(true).
      source(super.getVertex _).
      build()
    
    abstract override def getVertex(value: Value): Vertex = vertexCache.get(value)
  }
}

object CachingGraphSail {
  implicit  def FunctionAsCacheSource[A,B](f: A=>B): CacheSource[A, B] = new CacheSource[A,B] {
    override def get(k: A): B = f(k)
  }
}