package org.cardioart.rdv

/**
 * This classes represent the exception used in each actors
 */
@SerialVersionUID(1L)
class NetworkConnectionException(msg: String) extends Exception(msg) with Serializable

@SerialVersionUID(1L)
class StreamConnectionException(msg: String) extends Exception(msg) with Serializable