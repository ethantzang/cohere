# Java HTTP Simple Reverse Proxy

This repo provides a simple implementation of an HTTP reverse proxy in Java utilizing socket
programing. 

## Getting Started

NOTE: Ensure that Java Development Kit (JDK) 8 or higher is installed on your system.

Clone this repository or download the `ReverseProxy.java` file.

### Compilation

To compile the Java files:

1. Open up a terminal  
2. Navigate to the directory containing the Java files
3. Compile the main class:

    ```shell
    javac ReverseProxy.java
    ```
### Running the Proxy

To run the compiled program:

```shell
java ReverseProxy
```

By default, the reverse proxy will start running on port 8080, forwarding 
requests to example.com:80. You can modify these settings in the main
method of the ReverseProxy class.


### Implementation Details

1. Multi-threading: The proxy uses a thread pool to handle multiple concurrent
   connections, improving performance and resource management.
2. Buffered I/O: We use buffered reads and writes to efficiently transfer data
   between the client and the target server.
3. Configurable settings: The proxy port, target host, and target port can be
   specified when creating a ReverseProxy instance.

### Limitation and Scaling Consideration

This simple HTTP reverse proxy server is a bootstrap project there's a lot to be evolved:

* **No Load Balancing**: The current setup does not include load balancing features. All requests
  for a tenant are directed to a single target server.

  **Considerations**
   *  Implement load balancing to distribute requests across multiple target servers per tenant.
      such as round-robin or weighted balancing can be applied
  
* **No Rate Limiting**

  **Considerations**
   *  Implement rate limiting to mitigate abuse and protect the proxy from being overwhelmed by
      excessive requests

* **Single-threaded Request Handling**: Each request is handled sequentially by a single thread.
  This could lead to delays if the proxy server is processing multiple requests simultaneously.

  **Considerations**
   *  Implement async processing such as on-blocking I/O options (e.g., NIO or Netty) to improve
      performance and scalability

* **Limited Scalability**: The current implementation uses a fixed-size thread pool. 
   As traffic increases, the thread pool might become a bottleneck. The system is not
   designed to handle very high loads or large numbers of concurrent connections efficiently.

   **Considerations**: 
   *  Tuning thread pool size, adjusting the size to handle more concurrent connections.
   *  Using `ThreadPoolExecutor` with dynamic resizing and bounded queue 

* **Basic Error Handling**: The error handling is minimal and may not cover all edge cases,
   leading to potential ungraceful error responses.

For further consideration to production-ize there are two major parts to consider:

#### Security

* Validate and sanitize the incoming request before processing further
* Setup SSL/TLS for strong transmission
* Authentication, Authorization and Auditing aspect need to be in place, such as API keys Oauth token
   to further have access controls restrict the access to prevent un-identified and unauthorized access
   and the audit log for analysis and compliance purpose

#### Reliability & Production

* Testing infra, unit testing, integration testing and E2E testing to ensure the proxy can continuously
   evolving and simplify the maintenance overhead
* Monitoring, Logging and Alerting: Set up systems to track performance, identify issues, and 
   ensure observability. Consider using tools such as Prometheus, Grafana, or the ELK Stack.
* Deployment, clusters resource capacity estimation to have a plan for clustered deployment of 
   multiple proxy instances behind a load balancer to manage high traffic volumes and ensure
   fault tolerance.

