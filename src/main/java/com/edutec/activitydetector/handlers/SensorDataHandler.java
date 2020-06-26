package com.edutec.activitydetector.handlers;

import com.edutec.activitydetector.bindings.Bindings;
import com.edutec.activitydetector.model.AccelerometerRecord;
import lombok.RequiredArgsConstructor;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@EnableBinding(Bindings.class)
@Component
@RequiredArgsConstructor
public class SensorDataHandler {

//    private final ObjectMapper mapper;
    Logger log = LogManager.getLogger();

    @StreamListener(Bindings.SENSOR_DATA)
    @SendTo(Bindings.ACTIVITIES)
    public KStream<String, String> process(KStream<String, AccelerometerRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                        "', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            return value;
        }).groupByKey().count().mapValues(String::valueOf).toStream();
    }

}
