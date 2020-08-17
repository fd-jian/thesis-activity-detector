package de.dipf.edutec.thriller.experiencesampling.detector.handlers;

import de.dipf.edutec.thriller.experiencesampling.detector.bindings.Bindings;
import de.dipf.edutec.thriller.experiencesampling.CountSumTime;
import de.dipf.edutec.thriller.experiencesampling.SensorRecord;
import de.dipf.edutec.thriller.experiencesampling.SensorRecordRounded;
import de.dipf.edutec.thriller.experiencesampling.Stats;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @SendTo(Bindings.STATS)
    public KStream<String, Stats> process(KStream<String, SensorRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            //log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    //"', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");

            return value;
        }).groupByKey().aggregate(
                () -> new CountSumTime(0L, roundAndFormatTime.apply(0F), "", Instant.now().toEpochMilli(), ""),
                (key, value, aggregate) -> {
                    aggregate.setTime(DateTimeFormatter.ofPattern("hh:mm:ss:SSS")
                            .format(ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(value.getTime()), ZoneId.systemDefault())));

                    long currentTime = Instant.now().toEpochMilli();
                    float sinceLast = (currentTime - aggregate.getPrevTime()) / (float) 1000;
                    aggregate.setPrevTime(currentTime);

                    if(sinceLast < 5) {
                        aggregate.setCount(aggregate.getCount() + 1);
                        aggregate.setTimeSumSec(roundAndFormatTime.apply(
                                Float.parseFloat(aggregate.getTimeSumSec()) + sinceLast));
                    } else {
                        aggregate.setCount(0L);
                        aggregate.setTimeSumSec(roundAndFormatTime.apply(0F));
                    }
                    aggregate.setSessionId(value.getSessionId());

                    return aggregate;
                }).mapValues(this::newCountSumTimeAverage).toStream();
    }

    @StreamListener(Bindings.SENSOR_DATA2)
    @SendTo(Bindings.SENSOR_DATA_ROUNDED)
    public KStream<String, SensorRecordRounded> processRounded(KStream<String, SensorRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            //log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    //"', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");
            return new SensorRecordRounded(value.getTime(), value.getSessionId(),
                    value.getValues().stream().map(roundAndFormatSensor).collect(Collectors.toList()));
        });
    }

    private Stats newCountSumTimeAverage(CountSumTime value) {
        final long count = value.getCount();
        final float timeSumSec = Float.parseFloat(value.getTimeSumSec());
        return new Stats(
                count,
                value.getTime(),
                roundAndFormatTime.apply(timeSumSec),
                roundAndFormatTime.apply((float) count / timeSumSec),
                value.getSessionId());
    }
}
