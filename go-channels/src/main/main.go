package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"runtime"
	"strconv"
	"time"
)

type Edge struct {
	I      int
	J      int
	Weight int
}

type graph struct {
	edges    []Edge
	vertices int
}

type builder struct {
	data map[int]map[int]int
}

func (builder builder) build() graph {
	graph := graph{[]Edge{}, len(builder.data)}
	for source, neighbors := range builder.data {
		for target, weight := range neighbors {
			graph.edges = append(graph.edges, Edge{source, target, weight})
		}
	}
	return graph
}

func assembleGraph() graph {
	edgesAmount, limit := fetchEdgesAmount(), 1000
	batches := edgesAmount / limit
	if edgesAmount%limit > 0 {
		batches++
	}
	edgesRange := make(chan []Edge, runtime.NumCPU()*8)
	for i := 0; i < batches; i++ {
		go fetchEdges(i*limit, limit, edgesRange)
	}
	builder := builder{map[int]map[int]int{}}
	for i := 0; i < batches; i++ {
		edges := <-edgesRange
		for _, edge := range edges {
			neighbors, contains := builder.data[edge.I]
			if !contains {
				builder.data[edge.I] = map[int]int{}
				neighbors = builder.data[edge.I]
			}
			neighbors[edge.J] = edge.Weight
		}
	}

	return builder.build()
}

func fetchEdgesAmount() int {
	resp, _ := http.Get("http://localhost:8080/api/graph/edges-quantity")
	defer resp.Body.Close()
	content, _ := ioutil.ReadAll(resp.Body)
	edgesAmount, _ := strconv.ParseInt(string(content), 10, 32)
	return int(edgesAmount)
}

func fetchEdges(offset int, limit int, channel chan<- []Edge) {
	var edges []Edge
	done, url := false, fmt.Sprintf(
		"http://localhost:8080/api/graph?offset=%d&limit=%d",
		offset, limit)
	for resp, err := http.Get(url); !done; resp, err = http.Get(url) {
		if err != nil {
			fmt.Println(err.Error())
			os.Exit(0)
		}
		done = func() bool {
			defer resp.Body.Close()
			if resp.StatusCode == 200 {
				body, err := ioutil.ReadAll(resp.Body)
				if err != nil {
					fmt.Println(err.Error())
					os.Exit(0)
				}
				err = json.Unmarshal(body, &edges)
				if err != nil {
					fmt.Println(err.Error())
					os.Exit(0)
				}
				channel <- edges
			}
			return resp.StatusCode == 200
		}()
	}
}

func main() {
	begin := time.Now()

	graph := assembleGraph()
	fmt.Println("Vertices:", graph.vertices)
	fmt.Println("Edges:", len(graph.edges))
	fmt.Println("Milliseconds taken:", int(time.Since(begin).Nanoseconds()/1000000))
}
