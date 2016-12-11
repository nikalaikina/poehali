package com.github.nikalaikina.poehali.to

import com.github.nikalaikina.poehali.dao.DbModel
import com.github.nikalaikina.poehali.model.Flight

case class JsonRoute(flights: List[Flight], id: Option[String] = None) extends DbModel
