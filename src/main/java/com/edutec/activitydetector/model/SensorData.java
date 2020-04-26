package com.edutec.activitydetector.model;

import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.SchemaBuilder;

// TODO: make use of as soon as data is not streamed as byte array anymore
public class SensorData {
    private ConnectSchema schema;
    private Byte[] payload;
}
