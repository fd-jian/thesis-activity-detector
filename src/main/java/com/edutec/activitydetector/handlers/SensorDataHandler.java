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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@EnableBinding(Bindings.class)
@Component
@RequiredArgsConstructor
public class SensorDataHandler {

//    private final ObjectMapper mapper;
    Logger log = LogManager.getLogger();

    private final DecimalFormat dfSensor = new DecimalFormat(" #,#00.000000;-#");
    private final DecimalFormat dfTimeSec = new DecimalFormat("#000.00");

    private final Function<Float, Float> roundSensor = (f) ->
            Math.round(f * 100000000L) / (float) 100000000L;

    private final Function<Float, Float> roundTime = (f) ->
            Math.round(f * 100) / (float) 100;

    private final Function<Float, String> roundAndFormatSensor = (f) ->
            dfSensor.format(roundSensor.apply(f));

    private final Function<Float, String> roundAndFormatTime = (f) ->
            dfTimeSec.format(roundTime.apply(f));

    @StreamListener(Bindings.SENSOR_DATA)
    @SendTo(Bindings.ACTIVITIES)
    public KStream<String, CountSumTimeAverage> process(KStream<String, AccelerometerRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            //log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    //"', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            return value;
        }).groupByKey().aggregate(
                () -> new CountSumTime(0L, roundAndFormatTime.apply(0F), "", Instant.now().toEpochMilli()),
                (key, value, aggregate) -> {

                    aggregate.setTime(DateTimeFormatter.ofPattern("hh:mm:ss:SSS")
                            .format(ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(value.getTime()), ZoneId.systemDefault())));

                    long currentTime = Instant.now().toEpochMilli();
                    float sinceLast = (currentTime - aggregate.getPrevTime()) / (float) 1000;
                    aggregate.setPrevTime(currentTime);

                    if(sinceLast < 15) {
                        aggregate.setCount(aggregate.getCount() + 1);
                        aggregate.setTimeSumSec(roundAndFormatTime.apply(
                                Float.parseFloat(aggregate.getTimeSumSec()) + sinceLast));
                    } else {
                        aggregate.setCount(0L);
                        aggregate.setTimeSumSec(roundAndFormatTime.apply(0F));
                    }

                    return aggregate;
                }).mapValues(this::newCountSumTimeAverage).toStream();

    }

    @StreamListener(Bindings.SENSOR_DATA2)
    @SendTo(Bindings.SENSOR_DATA_ROUNDED)
    public KStream<String, AccelerometerRecordRounded> processRounded(KStream<String, AccelerometerRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            //log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    //"', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            Float[] values = value.getValues().toArray(new Float[0]);
            return new AccelerometerRecordRounded(value.getTime(),
                    roundAndFormatSensor.apply(values[0]),
                    roundAndFormatSensor.apply(values[1]),
                    roundAndFormatSensor.apply(values[2]));
        });

    }

    private CountSumTimeAverage newCountSumTimeAverage(CountSumTime value) {
        final long count = value.getCount();
        final float timeSumSec = Float.parseFloat(value.getTimeSumSec());
        return new CountSumTimeAverage(
                count,
                value.getTime(),
                roundAndFormatTime.apply(timeSumSec),
                roundAndFormatTime.apply((float) count / timeSumSec));
    }

}
