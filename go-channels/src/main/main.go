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

	"github.com/parnurzeal/gorequest"
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
	data        map[int]map[int]int
	edgesAmount int
}

func buildGraph(builder builder) graph {
	graph := graph{[]Edge{}, len(builder.data)}
	for source, neighbors := range builder.data {
		for target, weight := range neighbors {
			graph.edges = append(graph.edges, Edge{source, target, weight})
		}
	}
	return graph
}

func fetchEdgesAmount(client *gorequest.SuperAgent) int {
	_, body, _ := client.Get("http://localhost:8080/api/graph/edges-quantity").End()
	edgesAmount, _ := strconv.ParseInt(body, 10, 32)
	return int(edgesAmount)
}

func fetchEdges(offset int, limit int, channel chan<- []Edge, client *gorequest.SuperAgent) {
	var edges []Edge
	time.Sleep(2 * time.Second)
	resp, err := http.Get(fmt.Sprintf(
		"http://localhost:8080/api/graph?offset=%d&limit=%d",
		offset, limit))
	if err != nil {
		fmt.Println(err.Error())
		os.Exit(0)
	}
	defer resp.Body.Close()
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

func main() {
	begin := time.Now()
	httpClient := gorequest.New()

	edgesAmount, limit := fetchEdgesAmount(httpClient), 1000
	batches := edgesAmount / limit
	if edgesAmount%limit > 0 {
		batches++
	}
	edgesRange := make(chan []Edge, runtime.NumCPU()*8)
	for i := 0; i < batches; i++ {
		go fetchEdges(i*limit, limit, edgesRange, httpClient)
	}
	builder := builder{map[int]map[int]int{}, 0}
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

	graph := buildGraph(builder)
	fmt.Println("Vertices:", graph.vertices)
	fmt.Println("Edges:", len(graph.edges))
	fmt.Println("Milliseconds taken:", int(time.Since(begin).Nanoseconds()/1000000))
}
