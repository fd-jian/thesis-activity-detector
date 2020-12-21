package de.dipf.edutec.thriller.experiencesampling.detector.handlers;

import de.dipf.edutec.thriller.experiencesampling.*;
import de.dipf.edutec.thriller.experiencesampling.detector.bindings.Bindings;
import io.confluent.kafka.streams.serdes.avro.PrimitiveAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnableBinding(Bindings.class)
@Component
@RequiredArgsConstructor
public class SensorDataHandler {
    private static final String SCHEMA_REGISTRY_URL_KEY = "schema.registry.url";
    private static final Logger log = LogManager.getLogger(SensorDataHandler.class);

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

    @Value("${spring.cloud.stream.kafka.streams.binder.configuration.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.cloud.stream.bindings.user-ids-in.destination}")
    private String userIdsInDestination;

    /**
     * Calculates real-time metrics for the Kafka topic that holds all sensors, namely time elapsed in the recording
     * session and sensor records per second.
     *
     * @param sensorDataStream
     * @return
     */
    @StreamListener(Bindings.STATS_IN)
    @SendTo(Bindings.STATS_OUT)
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

    /**
     * Produces a stream of rounded sensor records to simplify consumers presentation logic.
     *
     * @param sensorDataStream
     * @return
     */
    @StreamListener(Bindings.LINEAR_ACCELERATION_ROUNDED_IN)
    @SendTo(Bindings.LINEAR_ACCELERATION_ROUNDED_OUT)
    public KStream<String, SensorRecordRounded> processRounded(KStream<String, SensorRecord> sensorDataStream) {

        // TODO: implement activity recognition logic

        return sensorDataStream.mapValues((readOnlyKey, value) -> {
            //log.info("Retrieved message from input binding '" + Bindings.SENSOR_DATA +
                    //"', forwarding to output binding '" + Bindings.ACTIVITIES + "'.");
            return new SensorRecordRounded(value.getTime(), value.getSessionId(),
                    value.getValues().stream().map(roundAndFormatSensor).collect(Collectors.toList()));
        });
    }

    /**
     * <p>
     * Listens to the Kafka topic that contains records from all sensors to transform it into a stream containing
     * a snapshots of user IDs from all users. The resulting type of record is named {@link UserIdsStatus}
     * </p>
     * <p>
     * The stream of sensor records must be grouped by a constant value to be able to aggregate on all records. Since
     * sensor records use the user ID as the key, it must be moved to the value so that grouping by a constant value
     * does not cause the user ID to be lost.
     * </p>
     * <p>
     * After a stream of {@link UserIdsStatus} records is created, it is grouped and aggregated again to mark those
     * status objects that hold different IDs than the previous one. This aggregation allows to  subsequently filter
     * only status records with a changed set of user IDs, which results in a stream that emits a new record every time
     * a new user was found among the sensor records.
     * </p>
     *
     * @param sensorDataStream Stream holding records from all sensors.
     * @return Stream that emits  a new record every time a new user was found among sensor records.
     */
    @StreamListener(Bindings.USER_IDS_IN)
    @SendTo(Bindings.USER_IDS_OUT)
    public KStream<Long, UserIds> userIds(KStream<String, SensorRecord> sensorDataStream) {
        final Map<String, String> avroSerdeConfig = Collections.singletonMap(SCHEMA_REGISTRY_URL_KEY, schemaRegistryUrl);
        final PrimitiveAvroSerde<Long> longKeyPrimitiveSerde = new PrimitiveAvroSerde<>();
        longKeyPrimitiveSerde.configure(avroSerdeConfig, true);
        final SpecificAvroSerde<UserId> valueSerde = new SpecificAvroSerde<>();
        valueSerde.configure(avroSerdeConfig, false);
        final PrimitiveAvroSerde<String> stringKeyPrimitiveSerde = new PrimitiveAvroSerde<>();
        stringKeyPrimitiveSerde.configure(avroSerdeConfig, true);

        return sensorDataStream
                // SensorRecords have a user id as key. To access user IDs as values, the Stream is mapped so that
                // the user ID is both key and value of the objects.
                .mapValues((readOnlyKey, value) -> new UserId(stringKeyPrimitiveSerde.deserializer().deserialize(userIdsInDestination, readOnlyKey.getBytes())))
                // Group by "0" to put all user ID records into one group
                .groupBy((key, value) -> 0L, Grouped.with(longKeyPrimitiveSerde, valueSerde))
                // Aggregate all IDs into a single record, holding a Set of unique user IDs
                .aggregate(() -> new UserIdsStatus(new UserIds(0L, new ArrayList<>()), false),
                        (key, value, aggregate) -> {
                            UserIds userIds = aggregate.getUserIds();
                            Set<String> strings = new HashSet<>(userIds.getIds());
                            strings.add(value.getId());
                            userIds.setIds(new ArrayList<>(strings));
                            userIds.setTime(Instant.now().toEpochMilli()); return aggregate;
                }).toStream()
                // Key is still "0" for all records, thus all records containing a set of all user IDs will be grouped
                // together again
                .groupByKey()
                // Flag user ID status as changed if the set of ID differs from the previous one.
                .aggregate(
                        () -> new UserIdsStatus(new UserIds(0L, new ArrayList<>()), true),
                        (key, value, aggregate) -> {
                            if (!new HashSet<>(aggregate.getUserIds().getIds())
                                    .equals(new HashSet<>(value.getUserIds().getIds()))) {
                               value.setChanged(true);
                           }
                            return value;
                        }).toStream()
                // Only emit new user ID status records if the set of user IDs changed
                .filter((key, value) -> value.getChanged())
                // Get rid of "changed" property as for all remaining records `change = true` applies.
                .mapValues((readOnlyKey, value) -> {
                    UserIds userIds = value.getUserIds();
                    return new UserIds(userIds.getTime(), userIds.getIds());
                });

    }

    /**
     * Redirects linear acceleration records to the consolidated topics for all sensor types.
     *
     * @param sensorDataStream Stream holding linear acceleration records.
     * @return Stream holding records linear acceleration records.
     */
    @StreamListener(Bindings.ALL_LINEAR_ACCELERATION_IN)
    @SendTo(Bindings.ALL_LINEAR_ACCELERATION_OUT)
    public KStream<String, SensorRecord> allLinearAcceleration(KStream<String, SensorRecord> sensorDataStream) {
        return sensorDataStream;
    }

    /**
     * Redirects accelerometer records to the consolidated topics for all sensor types.
     *
     * @param sensorDataStream Stream holding records of accelerometer data.
     * @return Stream holding records of accelerometer data.
     */
    @StreamListener(Bindings.ALL_ACCELEROMETER_IN)
    @SendTo(Bindings.ALL_ACCELEROMETER_OUT)
    public KStream<String, SensorRecord> allAccelerometer(KStream<String, SensorRecord> sensorDataStream) {
        return sensorDataStream;
    }

    /**
     * Redirects gyroscope records to the consolidated topics for all sensor types.
     *
     * @param sensorDataStream Stream holding gyroscope records.
     * @return Stream holding records gyroscope records.
     */
    @StreamListener(Bindings.ALL_GYROSCOPE_IN)
    @SendTo(Bindings.ALL_GYROSCOPE_OUT)
    public KStream<String, SensorRecord> allGyroscope(KStream<String, SensorRecord> sensorDataStream) {
        return sensorDataStream;
    }

    /**
     * Redirects light records to the consolidated topics for all sensor types.
     *
     * @param sensorDataStream Stream holding light records.
     * @return Stream holding records light records.
     */
    @StreamListener(Bindings.ALL_LIGHT_IN)
    @SendTo(Bindings.ALL_LIGHT_OUT)
    public KStream<String, SensorRecord> allLight(KStream<String, SensorRecord> sensorDataStream) {
        return sensorDataStream;
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
