package com.edutec.activitydetector.handlers;

import com.edutec.activitydetector.bindings.Bindings;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public KStream<Byte[], Byte[]> handlePropartFeedback(KStream<Byte[], Byte[]> sensorDataStream) {

        // TODO: implement activity recognition logic

        log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                "', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

        return sensorDataStream;
    }
}
