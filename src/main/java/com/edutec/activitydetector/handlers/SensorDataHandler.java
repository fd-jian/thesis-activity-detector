package com.edutec.activitydetector.handlers;

import com.edutec.activitydetector.bindings.Bindings;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@EnableBinding(Bindings.class)
@Component
@RequiredArgsConstructor
public class SensorDataHandler {

//    private final ObjectMapper mapper;
    Logger log = LogManager.getLogger();

    @StreamListener(Bindings.SENSOR_DATA)
    @SendTo(Bindings.ACTIVITIES)
    public KStream<String, String> process(KStream<String, String> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                        "', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            return Arrays.stream(value.split(" "))
                    .map(SensorDataHandler::alternateLowerUppercase)
                    .collect(Collectors.joining(" "));
        });
    }

    private static String alternateLowerUppercase(String s) {
        char[] chars = s.toCharArray();
        char[] newChars = new char[chars.length];

        for (int i = 0; i < chars.length; i++) {
            char curChar = chars[i];
            newChars[i]  = i % 2 == 0 ?
                    Character.toLowerCase(curChar)
                    : Character.toUpperCase(curChar);
        }

        return String.valueOf(newChars);
    }
}
