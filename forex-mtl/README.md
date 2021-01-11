## Forex MTL Solution

*This is not an official documentation, more like notes.*

I see a variety of directions to improve the solution,
and some of them are pretty crucial as for me, e.g. tests, logging, code structuring.
Stopping work at this point, cause already worked on the exercise over the weekend.
Tests are the main thing I would focus on next.

Solution contains some comments marked with `Q: `.
Those are typically some biases or questions I usually raising during PR review process.
Leaving them in the code as additional notes.

### Focus Areas

Focus areas during solving:
* Provide relevant responses to the valid requests
* Properly treat [one-frame limitations](https://hub.docker.com/r/paidyinc/one-frame)
in case of successful/correct responses from the `one-frame` service

TODO areas:
* Tests  
Provided test is very bad and maybe I should have been removed it at all.  
Most likely, exists helper libraries to create tests for the Tagless Final solutions, I haven't checked yet.  
Logic of the provided test is not complete.
It should verify cached value returned, using arbitrary value of `Timestamp` from a set of not-expired times for a given cached rate.  
Also, parts of test initialization logic should be moved into separate functions.
* Better error handling  
I don't feel fully satisfied by the current error names & messages.  
Also, there are exceptional situations, not signaled by proper errors, e.g. `one-frame` service unavailable.  
As a separate note, having more time, I would implement error-to-http-response mapping using type class.
* Logging
* Retries (with circuit breaker) for the `one-frame` interactions  
In case of `one-frame` service unavailability, we also can provide outdated rates
(this should be signaled by the response structure), or just error
* Code structuring  
This mostly caused by my very small experience with Tagless Final.  
One of the points, I feel like `OneFrameCached` logic should probably go to the `programs` module.

### Assumptions & Simplifications

#### Application Mode

Provided solution targets single instance mode.
This simplification removes a need to use distributed cache.
Also, distributed mode brings a need to synchronize cache population,
which itself is a distributed problem (as synchronization should be done across multiple instances).

#### One-frame Behaviour Expectations

*This assumption based on the `one-frame`'s service behaviour, which I have observed.*

Solution assumes `one-frame` service will provide responses comparatively quickly, and in case of valid structure data will be fresh.  
This doesn't mean solution will fail to work in case of slow `one-frame` responses, but rates may be outdated (failed requirement) under certain conditions,
e.g. there is no checking for the case of `one-frame` service provides already outdated rates.  
For example, `one-frame` response contains rates with `time_stamp` more than 5 minutes back in time (as per requirements).  
In such cases solution will provide outdated rates to the API consumers.  
On the `one-frame` slow responses topic, if that's the case:
we can preload rates eagerly (from the `one-frame` service) regardless of a demand of the `forex` service API consumers.

### Running Application & Tests

#### Running Dependencies

**Prerequirements**:
* Installed and running `docker` engine

Take the next steps to run `one-frame` service:
1) Pull `one-frame` service docker image:
```shell
$ docker pull paidyinc/one-frame
```
2) Execute the next command in the terminal:
```shell
$ docker run -p 8081:8080 paidyinc/one-frame
```
For more details, go to a [one-frame image page](https://hub.docker.com/r/paidyinc/one-frame) 

#### Running Application

**Prerequirements**:
* Installed `sbt`

Take the next steps to run `forex` service:
1) In the terminal, navigate to the `forex-mtl` directory.
2) Execute the next command in the terminal:
```shell
$ sbt run
```

Application will listen for requests at port `8080`.
To run simple request execute the next command in the terminal:
```shell
$ curl 'localhost:8080/rates?from=USD&to=JPY'
```

#### Running Tests

**Prerequirements**:
* Installed `sbt`

Take the next steps to run tests of the `forex` service:
1) In the terminal, navigate to the `forex-mtl` directory.
2) Execute the next command in the terminal:
```shell
$ sbt test
```
