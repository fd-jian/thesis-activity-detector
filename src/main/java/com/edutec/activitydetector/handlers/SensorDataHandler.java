package com.edutec.activitydetector.handlers;

import com.edutec.activitydetector.bindings.Bindings;
import com.edutec.activitydetector.countsum.CountSumTime;
import com.edutec.activitydetector.countsum.CountSumTimeAverage;
import com.edutec.activitydetector.model.AccelerometerRecord;
import com.edutec.activitydetector.model.AccelerometerRecordRounded;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.function.Function;

@EnableBinding(Bindings.class)
@Component
@RequiredArgsConstructor
public class SensorDataHandler {

//    private final ObjectMapper mapper;
    Logger log = LogManager.getLogger();

    @StreamListener(Bindings.SENSOR_DATA)
    @SendTo(Bindings.ACTIVITIES)
    public KStream<String, CountSumTimeAverage> process(KStream<String, AccelerometerRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    "', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            return value;
        }).groupByKey().aggregate(
                () -> new CountSumTime(0L, 0F, Instant.now().toEpochMilli()),
                (key, value, aggregate) -> {
                    long currentTime = Instant.now().toEpochMilli();
                    float sinceLast = (currentTime - aggregate.getPrevTime()) / (float) 1000;
                    aggregate.setPrevTime(currentTime);
                    if(sinceLast < 15) {
                        aggregate.setCount(aggregate.getCount() + 1);
                        aggregate.setTimeSumSec(aggregate.getTimeSumSec() + sinceLast);
                    } else {
                        aggregate.setCount(0L);
                        aggregate.setTimeSumSec(0F);
                    }
                    return aggregate;
                }).mapValues(this::newCountSumTimeAverage).toStream();

    }

    @StreamListener(Bindings.SENSOR_DATA2)
    @SendTo(Bindings.SENSOR_DATA_ROUNDED)
    public KStream<String, AccelerometerRecordRounded> processRounded(KStream<String, AccelerometerRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    "', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            final DecimalFormat decimalFormat = new DecimalFormat(" #,#0.000000;-#");

            Function<Float, String> round = (f) ->
                    decimalFormat.format(Math.round(f * 1000000) / (float) 1000000);

            return new AccelerometerRecordRounded(value.getTime(),
                    round.apply(value.getX()),
                    round.apply(value.getY()),
                    round.apply(value.getZ()));
        });
    }

    private CountSumTimeAverage newCountSumTimeAverage(CountSumTime value) {
        final long count = value.getCount();
        final float timeSumSec = value.getTimeSumSec();
        return new CountSumTimeAverage(
                count,
                timeSumSec,
                (float) count / timeSumSec);
    }

}
