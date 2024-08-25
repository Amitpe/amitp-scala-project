package common

import com.google.common.cache.CacheBuilder

import java.util.concurrent.TimeUnit

trait Cache[K <: AnyRef, V <: AnyRef] {
  def getOrElseUpdate(key: K, compute: () => V): V
}

class LruCache[K <: AnyRef, V <: AnyRef](maxNumberOfEntries: Long, expireAfterWriteMinutes: Long) extends Cache[K, V] {


  // TODO: make the cache computation async so it won't block the app. It means that the value should be Future[V] instead of V
  private val cache = CacheBuilder.newBuilder()
    .maximumSize(maxNumberOfEntries)
    .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
    .build[K, V]()

  // Method to get a value from the cache or compute and update it if not present
  override def getOrElseUpdate(key: K, compute: () => V): V = {
    val value = cache.getIfPresent(key)
    if (value != null) {
      value
    } else {
      val newValue = compute()
      cache.put(key, newValue)
      newValue
    }
  }
}


/*
  To estimate the maxSize (max number of entries) we need to know:
    1. How many users do we have
    2. How may different IP -> domain mapping each user generates per day
    3. How much days the log file represents
 */
case class LruCacheConfig(numOfDifferentUsers: Int,
                          numOfUniqueIpToDomainMappingPerUser: Int,
                          numOfDaysTheLogFileRepresents: Int)

object LruCache {

  def buildCache[K <: AnyRef, V <: AnyRef](config: LruCacheConfig) =
    new LruCache[K, V](
      maxNumberOfEntries = config.numOfDifferentUsers * config.numOfUniqueIpToDomainMappingPerUser * config.numOfDaysTheLogFileRepresents,
      expireAfterWriteMinutes = 60
    )

  def aCacheForMediumCompany[K <: AnyRef, V <: AnyRef](): LruCache[K, V] =
    buildCache[K, V](LruCacheConfig(
      numOfDifferentUsers = 2000,
      numOfUniqueIpToDomainMappingPerUser = 10,
      numOfDaysTheLogFileRepresents = 7
    ))

}