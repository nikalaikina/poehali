package com.github.nikalaikina.poehali.util

import java.lang.Math._

import info.mukel.telegrambot4s.models.Location

object DistanceCalculator
{
  def distance(x: Location, y: Location) = {
    val theta = x.longitude - y.longitude
    val dist = sin(deg2rad(x.latitude)) * sin(deg2rad(y.latitude)) +
      cos(deg2rad(x.latitude)) * cos(deg2rad(y.latitude)) *
      cos(deg2rad(theta))
    rad2deg(acos(dist)) * 60 * 1.1515 * 1.609344
  }

  def deg2rad(deg: Double) = deg * Math.PI / 180.0
  def rad2deg(rad: Double) = rad * 180 / Math.PI
}
