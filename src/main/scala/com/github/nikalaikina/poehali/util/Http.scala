package com.github.nikalaikina.poehali.util

import com.sun.net.ssl.{HostnameVerifier, HttpsURLConnection}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams

object Http {
  def get2(url: String) = scala.io.Source.fromURL(url).mkString
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
    HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier() {
      override def verify(s: String, s1: String): Boolean = true
    })
    val httpClient = new DefaultHttpClient
    val httpParams = httpClient.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    httpClient.setParams(httpParams)
    httpClient
  }
}
