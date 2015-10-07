The goal here is to have a hands on experience in the different approaches used by different languages for concurrency, parallelism and functional programming in general. For that I'm using a simple, well-defined problem: a server holds a graph and provides an API to fetch it. Each client application must assemble the graph on it's side and output basic data about it.

The thing is that that graph is a big one, you can't fetch it at once. What the server does is to expose it's adjacency matrix as a paged list of 3-tuple as in `i, j, K`. `i` and `j` are vertices and `K` is the weight of the edge connecting those nodes. Also, the graph server is scaling bad; requests fail frequently.

After trying out differents languages, the idea is to filter some and then move to more complex problems.
