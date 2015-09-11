# creepy-graph-adventure
A hands-on experience on graph-building to compare different languages

### The problem
A misbehaved server provides an API to fetch a graph adjacency matrix as a paged list of 3-tuple (first node -> second node = edge weight). Each different client application must cope with this server to build the graph on its side.
This is a simple yet fun problem that may show how each language handles async IO, parallelism, concurrency, network failure and so on.
