package com.fortysevendeg.ninecards.services.persistence

object PersistenceExceptions {

  case class PersistenceException(
    message: String,
    cause: Option[Throwable] = None) extends RuntimeException(message) {
    cause map initCause
  }

}