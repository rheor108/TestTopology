import org.apache.hadoop.io.SequenceFile;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.SequenceFileBolt;
import org.apache.storm.hdfs.bolt.format.*;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

//import storm configuration packages
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;

//Create main class LogAnalyserStorm submit topology.
public class LogAnalyserStorm {
    public static void main(String[] args) throws Exception{



        RecordFormat format = new DelimitedRecordFormat().withFieldDelimiter("|");

// sync the filesystem after every 1k tuples
        SyncPolicy syncPolicy = new CountSyncPolicy(1000);

// rotate files when they reach 5MB
        FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, FileSizeRotationPolicy.Units.MB);
/*
        FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                .withExtension(".seq")
                .withPath("/user/hive/warehouse/testtb1");

        // create sequence format instance.
        DefaultSequenceFormat format = new DefaultSequenceFormat("timestamp", "sentence");

        SequenceFileBolt bolt = new SequenceFileBolt()
                .withFsUrl("hdfs://kbank.big.dev:8020")
                .withFileNameFormat(fileNameFormat)
                .withSequenceFormat(format)
                .withRotationPolicy(rotationPolicy)
                .withSyncPolicy(syncPolicy)
                .withCompressionType(SequenceFile.CompressionType.RECORD)
                .withCompressionCodec("deflate");
*/

        FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                .withPath("/user/hive/warehouse/testtb2");

        HdfsBolt bolt = new HdfsBolt()
                .withFsUrl("hdfs://kbank.big.dev:8020")
                .withFileNameFormat(fileNameFormat)
                .withRotationPolicy(rotationPolicy)
                .withRecordFormat(format)
                .withSyncPolicy(syncPolicy);


        //Create Config instance for cluster configuration
        Config config = new Config();
        config.setDebug(true);

        //
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("call-log-reader-spout", new FakeCallLogReaderSpout());

        builder.setBolt("call-log-creator-bolt", new CallLogCreatorBolt())
                .shuffleGrouping("call-log-reader-spout");

        builder.setBolt("call-log-counter-bolt", bolt)
                .fieldsGrouping("call-log-creator-bolt", new Fields("call"));

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("LogAnalyserStorm", config, builder.createTopology());
        Thread.sleep(10000);

        //Stop the topology

        cluster.shutdown();
    }
}