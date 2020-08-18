package de.dipf.edutec.thriller.experiencesampling.detector.bindings;

import de.dipf.edutec.thriller.experiencesampling.SensorRecord;
import de.dipf.edutec.thriller.experiencesampling.SensorRecordRounded;
import de.dipf.edutec.thriller.experiencesampling.Stats;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;

public interface Bindings {

    String SENSOR_DATA = "sensor-data";
    String STATS = "stats";
    String SENSOR_DATA2 = "sensor-data2";
    String SENSOR_DATA_ROUNDED = "sensor-data-rounded";

    @Input(SENSOR_DATA)
    // TODO: use user id key
    KStream<String, SensorRecord> sensorData();

    @Output(STATS)
        // TODO: use user id key
    KStream<String, Stats> stats();

    @Input(SENSOR_DATA2)
    KStream<String, SensorRecord> sensorData2();

    @Output(SENSOR_DATA_ROUNDED)
    KStream<String, SensorRecordRounded> sensorDataRounded();

}
