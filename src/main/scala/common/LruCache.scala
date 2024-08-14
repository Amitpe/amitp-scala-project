package common

import com.google.common.cache.{Cache, CacheBuilder}

import java.util.concurrent.TimeUnit

class LruCache[K <: AnyRef, V <: AnyRef](maxSize: Long, expireAfterWriteMinutes: Long) {

  private val cache: Cache[K, V] = CacheBuilder.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
    .build()

  // Method to get a value from the cache or compute and update it if not present
  def getOrElseUpdate(key: K, compute: () => V): V = {
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