package org.example;

import org.apache.iotdb.flink.DefaultIoTSerializationSchema;
import org.apache.iotdb.flink.IoTDBSink;
import org.apache.iotdb.flink.IoTSerializationSchema;
import org.apache.iotdb.flink.options.IoTDBSinkOptions;
import org.apache.iotdb.tsfile.file.metadata.enums.CompressionType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;

import com.google.common.collect.Lists;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FlinkIoTDBSink {
    public static void main(String[] args) throws Exception {
        // run the flink job on local mini cluster
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        IoTDBSinkOptions options = new IoTDBSinkOptions();
        options.setHost("127.0.0.1");
        options.setPort(6667);
        options.setUser("root");
        options.setPassword("root");

        // If the server enables auto_create_schema, then we do not need to register all timeseries
        // here.
        options.setTimeseriesOptionList(
                Lists.newArrayList(
                        new IoTDBSinkOptions.TimeseriesOption(
                                "root.sg.d1.s1", TSDataType.DOUBLE, TSEncoding.GORILLA, CompressionType.SNAPPY)));

        IoTSerializationSchema serializationSchema = new DefaultIoTSerializationSchema();
        IoTDBSink ioTDBSink =
                new IoTDBSink(options, serializationSchema)
                        // enable batching
                        .withBatchSize(10)
                        // how many connections to the server will be created for each parallelism
                        .withSessionPoolSize(3);

        env.addSource(new SensorSource())
                .name("sensor-source")
                .setParallelism(1)
                .addSink(ioTDBSink)
                .name("iotdb-sink");

        env.execute("iotdb-flink-example");
    }

    private static class SensorSource implements SourceFunction<Map<String, String>> {
        boolean running = true;
        Random random = new SecureRandom();

        @Override
        public void run(SourceContext context) throws Exception {
            while (running) {
                Map<String, String> tuple = new HashMap();
                tuple.put("device", "root.sg.d1");
                tuple.put("timestamp", String.valueOf(System.currentTimeMillis()));
                tuple.put("measurements", "s1");
                tuple.put("types", "DOUBLE");
                tuple.put("values", String.valueOf(random.nextDouble()));

                context.collect(tuple);
                Thread.sleep(1000);
                System.out.println("Send Entry..");
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}

