package com.rexen.rlp;

import static com.hankcs.hanlp.utility.Predefine.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hankcs.hanlp.algoritm.MaxHeap;
import com.hankcs.hanlp.corpus.io.IIOAdapter;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.hankcs.hanlp.utility.Predefine;

public class RLP {
	
	public static final class Config {
		
		public static String StopPhrasePath = "";
		
		public static String SpiltPunctuation[] = new String[] { ",", "，", "、" };

		public static boolean ShowTermNature = true;

		public static IIOAdapter IOAdapter;
		
		public static final String MARK_FOR_SPLIT = String.valueOf(((char)174));
		
		public static final String MARK_FOR_PROTECTION = String.valueOf((char)178);
		
		public static final String RESTORE_CHAR = ",";
		
		//public static final Set<String> FilterTerms = new HashSet<String>();
		
		public static final int FilterFlag = 1;
		
		public static final int ReduceFlag = 2;
		
		public static final int ClauseFilterFlag = 4;
		
		public static final int Regex = 8;
		
		public static HashMap<String, Integer> FilterAndReduceFlage = new HashMap<String, Integer>();

		static {
			// 自动读取配置
			Properties p = new Properties();
			try {
				/////////////////////////////////////////////////////////////////////////////////
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				if (loader == null) {
					loader = RLP.Config.class.getClassLoader();
				}
				p.load(new InputStreamReader(Predefine.HANLP_PROPERTIES_PATH == null ? loader.getResourceAsStream("hanlp_rlp.properties") : new FileInputStream(Predefine.HANLP_PROPERTIES_PATH), "UTF-8"));
				String root = p.getProperty("root", "").replaceAll("\\\\", "/");
				if (!root.endsWith("/"))
					root += "/";
				
				StopPhrasePath = root + p.getProperty("StopPhraseWord", StopPhrasePath);
				HashMap<String, Integer> stopPhrase2FlagMap = readFileByLines(StopPhrasePath);
				
				//ShowTermNature = "true".equals(p.getProperty("ShowTermNature", "true"));
				
				//FilterAndReduceFlag = Integer.valueOf(p.getProperty("FilterAndReduce", "0"));
				
				for(String s : stopPhrase2FlagMap.keySet()){
					Config.FilterAndReduceFlage.put(backupContentWithParenthese(s), stopPhrase2FlagMap.get(s));
				}
				
				String t = p.getProperty("SpiltPunctuation","");
				SpiltPunctuation = t.split("\\|\\$");
				/////////////////////////////////////////////////////////////////////////////////
			} catch (Exception e) {
				logger.log(Level.WARNING, "Exception is", e);
				StringBuilder sbInfo = new StringBuilder("========Tips========\n请将hanlp_rlp.properties放在下列目录：\n"); // 打印一些友好的tips
				String classPath = (String) System.getProperties().get("java.class.path");
				if (classPath != null) {
					for (String path : classPath.split(File.pathSeparator)) {
						if (new File(path).isDirectory()) {
							sbInfo.append(path).append('\n');
						}
					}
				}
				sbInfo.append("Web项目则请放到下列目录：\n" + "Webapp/WEB-INF/lib\n" + "Webapp/WEB-INF/classes\n" + "Appserver/lib\n" + "JRE/lib\n");
				sbInfo.append("并且编辑root=PARENT/path/to/your/data\n");
				sbInfo.append("现在RLP将尝试从").append(System.getProperties().get("user.dir")).append("读取data……");
				logger.severe("没有找到hanlp_rlp.properties，可能会导致找不到data\n" + sbInfo);
			}
		}
	}
	
	public static Map<String, Float> getRankBySize(Map<String, Float> map, int size){
		Map<String, Float> result = new LinkedHashMap<String, Float>();
        for (Map.Entry<String, Float> entry : new MaxHeap<Map.Entry<String, Float>>(size, new Comparator<Map.Entry<String, Float>>()
        {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2)
            {
                return o1.getValue().compareTo(o2.getValue());
            }
        }).addAll(map.entrySet()).toList())
        {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
	}
	
	private static String replaceMarks(String content, String mark){
		for(int i = 0; i < RLP.Config.SpiltPunctuation.length; ++ i){
        	if(!RLP.Config.SpiltPunctuation[i].isEmpty()){
        		content = content.replaceAll(RLP.Config.SpiltPunctuation[i], mark);
        	}
        }
		return content;
	}
	
	public static String backupContentWithParenthese(String content){
		Pattern p = Pattern.compile("(?<=\\()[^\\)]+|(?<=（)[^）]+");
		Matcher m = p.matcher(content);
		ArrayList<String> strs = new ArrayList<String>();
        while (m.find()) {
            strs.add(m.group());            
        }
        for(String s : strs){
        	content = content.replaceAll(s, replaceMarks(s, Config.MARK_FOR_PROTECTION));
        }
        return content;
	}
	
	public static String restoreContentWithParenthese(String content, String mark){
		content = content.replaceAll(Config.MARK_FOR_PROTECTION, mark);
		return content;
	}
	
	public static String filterReduce(String seg) {
		boolean subFinish = false;
		String temp = seg;
		
		for (String s : Config.FilterAndReduceFlage.keySet()) {
			
			if ((Config.FilterAndReduceFlage.get(s) & Config.Regex) == Config.Regex) {
				Pattern p2 = Pattern.compile(s);
				Matcher m2 = p2.matcher(seg);
				if (m2.find()) {
					
					if (!subFinish && (Config.FilterAndReduceFlage.get(s) & Config.FilterFlag) == Config.FilterFlag) {
						seg = "";
						logger.log(Level.INFO, String.format("Regex Filter one seg %s --- %s ==> %s", temp, s, seg));
						System.out.println(String.format("Regex Filter one seg %s --- %s ==> %s", temp, s, seg));
						subFinish = true;
					}
					
					if ((Config.FilterAndReduceFlage.get(s) & Config.ReduceFlag) == Config.ReduceFlag) {
						seg = seg.replaceAll(s, "");
						logger.log(Level.INFO, String.format("Regex Reduce one seg %s --- %s ==> %s", temp, s, seg));
						System.out.println(String.format("Regex Reduce one seg %s --- %s ==> %s", temp, s, seg));
					}
					
					if (!subFinish && (Config.FilterAndReduceFlage.get(s) & Config.ClauseFilterFlag) == Config.ClauseFilterFlag) {
						seg = "";
						logger.log(Level.INFO, String.format("Regex Clause filter one seg %s --- %s ==> %s", temp, s, seg));
						System.out.println(String.format("Regex Clause filter one seg %s --- %s ==> %s", temp, s, seg));
						subFinish = true;
					}
				}
			}
		}
		// 双向查询
		if (Config.FilterAndReduceFlage.containsKey(seg)) {
			if ((Config.FilterAndReduceFlage.get(seg) & Config.FilterFlag) == Config.FilterFlag) {
				seg = "";
				logger.log(Level.INFO, String.format("Filter one seg %s --- %s ==> %s", temp, temp, seg));
				System.out.println(String.format("Filter one seg %s --- %s ==> %s", temp, temp, seg));
				subFinish = true;
			}
		}

		if (!subFinish) {
			for (String s : Config.FilterAndReduceFlage.keySet()) {
				if ((Config.FilterAndReduceFlage.get(s) & Config.Regex) != Config.Regex && seg.contains(s) && seg.length() > s.length()) {

					if ((Config.FilterAndReduceFlage.get(s) & Config.ReduceFlag) == Config.ReduceFlag) {
						seg = seg.replaceAll(s, "");
						logger.log(Level.INFO, String.format("Reduce one seg %s --- %s ==> %s", temp, s, seg));
						System.out.println(String.format("Reduce one seg %s --- %s ==> %s", temp, s, seg));
					}

					if (!subFinish && (Config.FilterAndReduceFlage.get(s)
							& Config.ClauseFilterFlag) == Config.ClauseFilterFlag) {
						seg = "";
						logger.log(Level.INFO, String.format("Clause filter one seg %s --- %s ==> %s", temp, s, seg));
						System.out.println(String.format("Clause filter one seg %s --- %s ==> %s", temp, s, seg));
						subFinish = true;
					}
				}
			}
		}
		return seg;
	}
	
	public static HashMap<String, Integer> readFileByLines(String fileName) {
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String str = null;
			int index = 1;

			while ((str = reader.readLine()) != null) {
				String[] tempArray = str.split(":");
				result.put(tempArray[0], Integer.valueOf(tempArray[1]));
				System.out.println("phrase index " + index + ": " + str);
				index++;
			}
			reader.close();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
		return result;
	}
	
	public static List<String> extractKeyword2(String text$1, int size){
		text$1 = backupContentWithParenthese(text$1);
		text$1 = replaceMarks(text$1, Config.MARK_FOR_SPLIT);

        String phraseArray[] = text$1.split(Config.MARK_FOR_SPLIT);
        List<Term> list = new ArrayList<Term>();
        for(int c = 0; c < phraseArray.length; ++ c){
        	String seg = filterReduce(phraseArray[c].trim());
			if (!seg.isEmpty()) {
				Term t = new Term(seg, Nature.n);
				list.add(t);
			}
        }
        //filter(list);
        TextRankKeyword textRankKeyword = new TextRankKeyword();
        Set<Map.Entry<String, Float>> entrySet = getRankBySize(textRankKeyword.getRank(list), size).entrySet();
        System.out.println(entrySet);
        List<String> result = new ArrayList<String>(entrySet.size());
        for (Map.Entry<String, Float> entry : entrySet)
        {
            result.add(restoreContentWithParenthese(entry.getKey(), Config.RESTORE_CHAR));
        }
        return result;
	}
	
	public static void main(String args[]){
		
		System.out.println("=========test run success=========");
		
		String txt_1 = "产业投资;产业投资;产业投资;产业投资;google,google,google,中医药产业投资；中药材种植及初加工；鹿与林蛙养殖；中医药技术研发；医药制造；旅游开发；养老服务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".化学药制剂、抗生素、生物制品（限诊断药品）、体外诊断试剂、吉林.产业投资;321医用电子仪器设备、google,240/340临床检验分析仪器、345体外循环及血液处理设备、346植入材料和人工器官、产业投资;364医用卫生材料及敷料、266/366医用高分子材料及制品、377介入器材批发、零售（医疗器械经营企业许可证有效期至2015年4月22日、药品经营许可证有效期至2015年4月22日，企业应在许可证有效期内开展经营活动）；以及化妆品、预包装食品（调味品、饮料）、日用品、保健食品的批发和进出口业务（食品流通许可证有效期至2013年7月11日）；医药产品、医药产品注册的咨询服务及医药领域软件开发#（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".吉林.从事中成药.google"
				+ ".商务信息咨询、投资信息咨询、企业管理信息咨询、计算机技术咨询（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".以自有资金对中医药、农副产品、旅游业、畜牧业项目进行投资。（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".中医药产业投资；中药材种植及初加工；鹿与林蛙养殖；中医药技术研发；医药制造；旅游开发；养老服务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）";

		System.out.println("\r\n\r\n"+RLP.extractKeyword2(txt_1, 5));
	}
	
}
