package de.dipf.edutec.thriller.experiencesampling.detector.bindings;

import de.dipf.edutec.thriller.experiencesampling.*;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;

public interface Bindings {

    String STATS_IN = "stats-in";
    String STATS_OUT = "stats-out";
    String LINEAR_ACCELERATION_ROUNDED_IN = "linear-acceleration-rounded-in";
    String LINEAR_ACCELERATION_ROUNDED_OUT = "linear-acceleration-rounded-out";

    String ALL_LINEAR_ACCELERATION_IN = "all-linear-acceleration-in";
    String ALL_LINEAR_ACCELERATION_OUT = "all-linear-acceleration-out";

    String ALL_ACCELEROMETER_IN = "all-accelerometer-in";
    String ALL_ACCELEROMETER_OUT = "all-accelerometer-out";

    String ALL_GYROSCOPE_IN = "all-gyroscope-in";
    String ALL_GYROSCOPE_OUT = "all-gyroscope-out";

    String ALL_LIGHT_IN = "all-light-in";
    String ALL_LIGHT_OUT = "all-light-out";

    String USER_IDS_IN = "user-ids-in";
    String USER_IDS_OUT = "user-ids-out";

    @Input(ALL_LINEAR_ACCELERATION_IN)
    KStream<String, SensorRecord> allLinearAccelerationIn();
    @Output(ALL_LINEAR_ACCELERATION_OUT)
    KStream<String, SensorRecord> allLinearAccelerationOut();

    @Input(ALL_ACCELEROMETER_IN)
    KStream<String, SensorRecord> allAccelerometerIn();
    @Output(ALL_ACCELEROMETER_OUT)
    KStream<String, SensorRecord> allAccelerometerOut();

    @Input(ALL_GYROSCOPE_IN)
    KStream<String, SensorRecord> allGyroscopeIn();
    @Output(ALL_GYROSCOPE_OUT)
    KStream<String, SensorRecord> allGyroscopeOut();

    @Input(ALL_LIGHT_IN)
    KStream<String, SensorRecord> allLightIn();
    @Output(ALL_LIGHT_OUT)
    KStream<String, SensorRecord> allLightOut();

    @Input(USER_IDS_IN)
    KStream<String, SensorRecord> userIdsIn();
    @Output(USER_IDS_OUT)
    KStream<Long, UserIds> userIdsOut();

    @Input(STATS_IN)
    KStream<String, SensorRecord> statsIn();
    @Output(STATS_OUT)
    KStream<String, Stats> statsOut();

    @Input(LINEAR_ACCELERATION_ROUNDED_IN)
    KStream<String, SensorRecord> linearAccelerationRoundedIn();
    @Output(LINEAR_ACCELERATION_ROUNDED_OUT)
    KStream<String, SensorRecordRounded> linearAccelerationRoundedOut();

}
