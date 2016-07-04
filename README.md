# How to run

sbt run

# Available endpoints

`GET /cities?number={number}` - returns an array of n most popular cities on skypicker with ids
`GET /flights?homeCities={comma-separated list}&cities={comma-separated list}&dateFrom={}&dateTo={}&daysFrom={}&daysTo={}&cost={}&citiesCount={}`

example: `/flights?homeCities=VNO,MSQ&cities=BUD,BGY,AMS,FCO&dateFrom=10/07/2016&dateTo=30/12/2016&daysFrom=8&daysTo=14&cost=500&citiesCount=2`
