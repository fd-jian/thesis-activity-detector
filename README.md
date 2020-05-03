# activity-detector
Activity detector for engagement detection system. Implemented in spring boot with spring-cloud-stream and kafka.  

(Basically just a skeleton application so far)  

## Build & Run

```
docker build -t [IMAGE_NAME] .
docker run -d --network=[NETWORK_NAME] -e SPRING_PROFILES_ACTIVE=snapshot [IMAGE_NAME]
```

Kafka instance must be available in the same network with hostname `kafka` and external port `29092`, otherwise the container will exit with failure. Consumes from a topic called `mqtt` and writes to a topic called `activities`. Must be key-value: String, String (subject to change).  

Currently these values are hardcoded into the application config. 
