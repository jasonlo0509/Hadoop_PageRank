package pagerank;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Input : <"\s"+ dangling node, pagerank + N >, <link, pagerank/C> <title, links>
 * Output : <Title, newPR | N || linkA,linkB,...>
 */

public class CalculateReducer  extends Reducer<Text, Text, Text, Text>{
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		
		String links = "";
		double sumShareOtherPageRanks = 0.0;
		double pageRank = 0.0;
		double DanglePageRank = 0.0;
		int N = 0;
		double dangleDivN = 0.0;
		String title = new String();
		
		for(Text val: values) {
			// count (sum pr of dangling nodes)/N
			if(key.toString().startsWith(" ")){
				String[] split = val.toString().split("\\t");
				DanglePageRank =  Double.parseDouble(split[0]);
				N = Integer.parseInt(split[1]);
				dangleDivN += DanglePageRank/N;
			}
			else {
				if(val.toString().startsWith("|")){
					Pattern rankPattern = Pattern.compile("|(.+?)||");
					Matcher rankMatcher = rankPattern.matcher( val.toString());
					while (rankMatcher.find()) {
						pageRank = Double.parseDouble(rankMatcher.group().substring(1,rankMatcher.group().length()-2));
					}
					
					Pattern linkPattern = Pattern.compile("||(.+?)");
					Matcher linkMatcher = linkPattern.matcher( val.toString());
					while(linkMatcher.find()) {
						links = linkMatcher.group().substring(2);
					}
					title = key.toString();
				}
				else {
					sumShareOtherPageRanks += Double.parseDouble(val.toString());
				}
			}
		}
		//count new PageRank
		double newRank = 0.15 / N + 0.85 * sumShareOtherPageRanks + 0.85 * dangleDivN;
		double err = newRank - pageRank;
		context.getCounter(Status.error).increment(err);
		String newLink = newRank + "|" + N + "||" + links;
		context.write(new Text(title), new Text(newLink));
	}
}
