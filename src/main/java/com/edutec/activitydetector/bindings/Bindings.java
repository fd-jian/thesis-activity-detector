package com.edutec.activitydetector.bindings;

import com.edutec.activitydetector.countsum.CountSumTimeAverage;
import com.edutec.activitydetector.model.AccelerometerRecord;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;

public interface Bindings {

    String SENSOR_DATA = "sensor-data";
    String ACTIVITIES = "activities";
    String SENSOR_DATA2 = "sensor-data2";
    String SENSOR_DATA_ROUNDED = "sensor-data-rounded";

    @Input(SENSOR_DATA)
    // TODO: use user id key
    KStream<String, AccelerometerRecord> sensorData();

    @Output(ACTIVITIES)
        // TODO: use user id key
    KStream<String, CountSumTimeAverage> activities();

    @Input(SENSOR_DATA2)
    KStream<String, AccelerometerRecord> sensorData2();

    @Output(SENSOR_DATA_ROUNDED)
    KStream<String, AccelerometerRecord> sensorDataRounded();

}
