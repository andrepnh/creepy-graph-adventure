An exercise I've used over the years for a quick hands-on experience with the different approaches used by different languages for concurrency, parallelism and data transformation. For that I'm using a simple, well-defined problem: a server holds a graph and provides an API to fetch it. Each client application must assemble the graph on it's side and output basic data about it.

The thing is that that graph is a big one, you can't fetch it all at once. What the server does is to expose it's adjacency matrix as a paged list of 3-tuple as in `i, j, K`. `i` and `j` are vertices and `K` is the weight of the edge connecting those nodes. Also, the graph server is scaling bad; requests fail frequently.

#### Clients implemented so far

* [Scala - Akka actors](https://doc.akka.io/docs/akka/current/actors.html): maybe an overkill for something so small, but the mix of immutability and coordination by messaging passaging is quite fun.
* [Go channels](https://gobyexample.com/channels): goroutines are great, but on hindsight [channels are not that good](https://www.jtolio.com/2016/03/go-channels-are-bad-and-you-should-feel-bad/) (special attention should be given to the survey mentioned on that post).
* [Clojure agents](https://clojure.org/reference/agents): in retrospect, agents seem more suitable for asynchronous uncoordinated work. Also, agents don't seem to be used that much by the clojure community. Although I already used channels on go, [core.async](https://clojure.org/news/2013/06/28/clojure-clore-async-channels) now seems like a better fit.
* RxJava: the 1.x version has a steep learning curve and I didn't find it that friendly. Changing this to [Reactor ](https://doc.akka.io/docs/akka/current/actors.html) or using newer rx would help a lot.
