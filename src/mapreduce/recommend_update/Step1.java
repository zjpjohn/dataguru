package mapreduce.recommend_update;

/*
 * 得到评分矩阵
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputCommitter;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.JobHistory.Values;


public class Step1 {
	public static class Step1_ToItemPreMapper extends MapReduceBase implements Mapper<Object, Text, IntWritable, Text>{
		private final static IntWritable k = new IntWritable();
		private final static Text v = new Text();
		
		public void map(Object key,Text value,OutputCollector<IntWritable, Text> output,Reporter reporter) throws IOException{
			String[] tokens = Recommend.DELIMITER.split(value.toString());
			int userID = Integer.parseInt(tokens[0]);//用户ID
			String itemID = tokens[1];//商品ID
			String pref = tokens[2];//评分
			k.set(userID);
			v.set(itemID + ":" + pref);
			output.collect(k, v);
		}
	}
	
	public static class Step1_ToUserVectorReducer extends MapReduceBase implements Reducer<IntWritable, Text, IntWritable, Text>{
		private final static Text v = new Text();
		
		public void reduce(IntWritable key,Iterator<Text> values,OutputCollector<IntWritable, Text> output,Reporter reporter) throws IOException{
			StringBuilder sb = new StringBuilder();
			while(values.hasNext()){
				sb.append("," + values.next());
			}
			v.set(sb.toString().replaceFirst("", ""));
			output.collect(key, v);
		}
	}
	
	public static void run(Map<String, String> path) throws IOException{
		JobConf conf = Recommend.config();
		
		String inputString = path.get("Step1Input");
		String outputString = path.get("Step1Output");
		
		HdfsDAO hdfs = new HdfsDAO(Recommend.HDFS,conf);
		hdfs.rmr(inputString);
		hdfs.mkdirs(inputString);
		hdfs.copyFile(path.get("data"), inputString);
		
		conf.setMapOutputKeyClass(IntWritable.class);
		conf.setMapOutputValueClass(Text.class);
		
		conf.setOutputKeyClass(IntWritable.class);
		conf.setOutputValueClass(Text.class);
		
		 conf.setMapperClass(Step1_ToItemPreMapper.class);
	     conf.setCombinerClass(Step1_ToUserVectorReducer.class);
	     conf.setReducerClass(Step1_ToUserVectorReducer.class);
	     
	     conf.setInputFormat(TextInputFormat.class);
	     conf.setOutputFormat(TextOutputFormat.class);
	     
	     FileInputFormat.setInputPaths(conf, new Path(inputString));
	     FileOutputFormat.setOutputPath(conf, new Path(outputString));
	     
	     RunningJob job = JobClient.runJob(conf);
	     while(!job.isComplete()){
	    	 job.waitForCompletion();
	     }
	}
}
