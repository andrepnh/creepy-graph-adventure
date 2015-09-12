## creepy-graph-adventure
The goal here is to try out different approaches for concurrency, parallelism and functional programming using different languages. For that I'm using a small, well-defined problem: a server holds a graph and provides an API to fetch it. Each client application must assemble the graph on it's side and output basic data about it.

The thing is that that graph is a big one, you can't fetch it at once. What the server does is to expose it's adjacency matrix as a paged list of 3-tuple as in `i, j, K`. `i` and `j` are vertices and `K` is the weight of the edge connecting those nodes. Also, the graph server is scaling bad; requests fail or "timeout" frequently.

With this small problem we can practice async IO, error handling, concurrency and parallelism.

#### Languages

Besides the graph server (a dropwizard app), I'm going to implement client apps on:

* Java 8 (probably backed by RxJava)
* Scala
* Go
* Clojure
* Node.js
