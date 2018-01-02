# Vertirest
Vert.x costumizable RESTful backend written in Kotlin.

Vertirest lets you to create a complete backand with REST operations, login, sign up, Facebook login and permissions with few lines of code.

## Initialization
In order to configure the backend a Vertirest object must be instantiated:
```kotlin
val vertirest = Vertirest(vertx, "mongodb://user:password@localhost:23333")
```
The first parameter is a Vertx instance, cause this library is based on Vert.x.
The second parameter is the address of a MongoDB database.

## Adding routes
What we want now is to have configured all of the main REST operation: GET (read), POST (create), DELETE (delete), PUT (update).
In order to do so first of all we must create a model class
```kotlin
class SamplePerson(
    var _id: String = "",
    var name: String? = null,
    var surname: String? = null,
    var age: Int = 0)
```
Then we add the route we want to vertirest
```kotlin
vertirest.configureRoutes("people", SamplePerson::class.java)
```

## Starting the server
Finally the server can be started with a custom or environmental port
```kotlin
val port = System.getenv("PORT") ?: "5050"
vertirest.startHttpServer(port.toInt(), "0.0.0.0")
```

## Executing RESTful operations
Nothing else. Once the server is on the followuing routes will be available:
- GET localhost:5050/people gives the list of all person documents.
- GET localhost:5050/people/:id gives a single person object.
- POST localhost:5050/people perform the creation of a new person.
- DELETE localhots:5050/people/:id deletes a single person object.
- DELETE localhost:5050/people deletes all the objects that match a query (passed inside body).
- PUT localhots:5050/people/:id updates a single person object.
- PUT localhost:5050/people updates all the objects that match a query (passed inside body).
