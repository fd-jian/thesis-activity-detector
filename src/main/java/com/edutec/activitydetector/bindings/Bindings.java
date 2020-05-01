package com.edutec.activitydetector.bindings;

import com.edutec.activitydetector.model.SensorData;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;

public interface Bindings {

    String SENSOR_DATA = "sensor-data";
    String ACTIVITIES = "activities";

    @Input(SENSOR_DATA)
    // TODO: use timestamp key
    KStream<String, String> sensorData();

    @Output(ACTIVITIES)
    // TODO: use timestamp key
    KStream<String, String> activities();
}
