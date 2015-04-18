package work.zookeeper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import public_class.hdfs.HdfsDAO;

/**
 * 销售
 * 
 * @author conan
 */
public class Sell2M {

    public static final String HDFS = "hdfs://192.168.142.133:9000";
    public static final Pattern DELIMITER = Pattern.compile("[\t,]");

    public static class SellMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        private String month = "2013-02";
        private Text k = null;
        private String keyString = null;
        private IntWritable v = new IntWritable();
        private int money = 0;

        public void map(LongWritable key, Text values, Context context) throws IOException, InterruptedException {
            System.out.println(values.toString());
            String[] tokens = DELIMITER.split(values.toString());
            if (tokens[3].startsWith(month)) {// 2月的数据
                money = Integer.parseInt(tokens[1]) * Integer.parseInt(tokens[2]);//单价*数量
                keyString = tokens[0];
                v.set(money);
                context.write(new Text(keyString), v);
            }
        }
    }

    public static class SellReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable v = new IntWritable();
        int maxMoney=0;

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int money = 0;
            for (IntWritable line : values) {
                // System.out.println(key.toString() + "\t" + line);
                money += line.get();
            }
            if(maxMoney < money){
            	maxMoney = money;
                v.set(money);
                context.write(key, v);
                System.out.println(key + "," + money);
            }
        }

    }

    public static void run(Map<String, String> path) throws IOException, InterruptedException, ClassNotFoundException {
        JobConf conf = config();
        String local_data = path.get("sell");
        String input = path.get("input");
        String output = path.get("output");

        // 初始化sell
        HdfsDAO hdfs = new HdfsDAO(HDFS, conf);
        hdfs.rmr(input);
        hdfs.mkdirs(input);
        hdfs.copyFile(local_data, input);

        Job job = new Job(conf);
        job.setJarByClass(Sell.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(SellMapper.class);
        job.setReducerClass(SellReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));

        job.waitForCompletion(true);
    }

    public static JobConf config() {// Hadoop集群的远程配置信息
        JobConf conf = new JobConf(Purchase.class);
        conf.setJobName("purchase");
        conf.addResource("classpath:/hadoop/core-site.xml");
        conf.addResource("classpath:/hadoop/hdfs-site.xml");
        conf.addResource("classpath:/hadoop/mapred-site.xml");
        return conf;
    }
    
    public static Map<String,String> path(){
        Map<String, String> path = new HashMap<String, String>();
        path.put("sell", "datafile/biz/sell.csv");// 本地的数据文件
        path.put("input", HDFS + "/exp/data/zookeeper/sell");// HDFS的目录
        path.put("output", HDFS + "/exp/result/zookeeper/sell"); // 输出目录
        return path;
    }

    public static void main(String[] args) throws Exception {
        run(path());
    }

}

