package com.edutec.activitydetector.bindings;

import com.edutec.activitydetector.model.AccelerometerRecord;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;

public interface Bindings {

    String SENSOR_DATA = "sensor-data";
    String ACTIVITIES = "activities";

    @Input(SENSOR_DATA)
    // TODO: use user id key
    KStream<String, AccelerometerRecord> sensorData();

    @Output(ACTIVITIES)
    // TODO: use user id key
    KStream<String, String> activities();
}
