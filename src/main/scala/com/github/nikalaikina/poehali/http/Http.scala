package com.github.nikalaikina.poehali.http

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{HttpClientBuilder, DefaultHttpClient}
import org.apache.http.params.HttpConnectionParams

object Http {
  def get(url: String,
          connectionTimeout: Int = 10000,
          socketTimeout: Int = 10000): String = {
    val httpClient = buildHttpClient(connectionTimeout, socketTimeout)
    val httpResponse = httpClient.execute(new HttpGet(url))
    val entity = httpResponse.getEntity
    var content = ""
    if (entity != null) {
      val inputStream = entity.getContent
      content = io.Source.fromInputStream(inputStream).getLines.mkString
      inputStream.close()
    }
    httpClient.getConnectionManager.shutdown()
    content
  }

  private def buildHttpClient(connectionTimeout: Int, socketTimeout: Int): DefaultHttpClient = {
    val httpClient = new DefaultHttpClient
    val httpParams = httpClient.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    httpClient.setParams(httpParams)
    httpClient
  }
}
