package ioinformatics.neo4j.rdf.plugin.optimization

import com.tinkerpop.blueprints.oupls.sail.GraphSail
import com.tinkerpop.blueprints.oupls.sail.GraphSail.DataStore
import com.tinkerpop.blueprints.{KeyIndexableGraph, Vertex}
import org.cache2k.{CacheBuilder}
import org.openrdf.model.{Value}
import CachingGraphSail._
import org.slf4j.LoggerFactory

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
trait CachingGraphSail[T <: KeyIndexableGraph] extends GraphSail[T] {

  private val log = LoggerFactory.getLogger(classOf[CachingGraphSail[_]])

  override def createStore(): DataStore[T] = if(cacheEnabled) {
    new DataStore[T] with CachedDataStore[T]
  }
  else {
    super.createStore()
  }

  trait CachedDataStore[T <: KeyIndexableGraph] extends  DataStore[T] {

    abstract override def getVertex(value: Value): Vertex = getVertexById(getFromCache(value))

    private  def getFromCache(key: Value): Object =
      Option(vertexCache.peek(key))
      .orElse(getVertexId(key).map(addToCache(key, _)))
      .orNull

    private def addToCache(key: Value, value: Object): Object = {
      log.debug(s"Adding to Vertex Cache: $key -> $value")
      vertexCache.put(key, value)
      value
    }

    private def getVertexId(value: Value): Option[Object] = Option(super.getVertex(value)).map(_.getId)
  }

  private def getVertexById(id: Object): Vertex = if(id == null) null else getBaseGraph.getVertex(id)

}

object CachingGraphSail {
  val vertexCache = CacheBuilder.newCache(classOf[Value], classOf[Object]).
    maxSize(cacheMaxSize).
    eternal(true).
    build()

  def cacheMaxSize: Int = System.getProperties.getProperty("vertexCache.maxSize", "1000000").toInt
  def cacheEnabled: Boolean = System.getProperties.getProperty("vertexCache.enabled", "true").toBoolean
}