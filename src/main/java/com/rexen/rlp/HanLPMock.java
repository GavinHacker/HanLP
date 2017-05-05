package com.rexen.rlp;

import static com.hankcs.hanlp.utility.Predefine.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.io.IIOAdapter;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.hankcs.hanlp.utility.Predefine;

public class HanLPMock {
	
	public static final class Config {
		
		public static String CoreStopWordDictionaryPath = "data/dictionary/stopwords.txt";

		public static String CRFDependencyModelPath = "data/model/dependency/CRFDependencyModelMini.txt";

		public static String CustomDictionaryPath[] = new String[] { "data/dictionary/custom/CustomDictionary.txt" };
		
		public static String SpiltPunctuation[] = new String[] { ",", "，", "、" };

		public static boolean ShowTermNature = true;

		public static IIOAdapter IOAdapter;
		
		public static final String MARK_FOR_SPLIT = String.valueOf(((char)174));
		
		public static final String MARK_FOR_PROTECTION = String.valueOf((char)178);
		
		public static final String RESTORE_CHAR = ",";
		
		public static final Set<String> FilterTerms = new HashSet<String>();

		static {
			// 自动读取配置
			Properties p = new Properties();
			try {
				/////////////////////////////////////////////////////////////////////////////////
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				if (loader == null) {
					loader = HanLP.Config.class.getClassLoader();
				}
				p.load(new InputStreamReader(Predefine.HANLP_PROPERTIES_PATH == null ? loader.getResourceAsStream("hanlp_rlp.properties") : new FileInputStream(Predefine.HANLP_PROPERTIES_PATH), "UTF-8"));
				String root = p.getProperty("root", "").replaceAll("\\\\", "/");
				if (!root.endsWith("/"))
					root += "/";
				CoreStopWordDictionaryPath = root + p.getProperty("CoreStopWordDictionaryPath", CoreStopWordDictionaryPath);

				String[] pathArray = p.getProperty("CustomDictionaryPath", "data/dictionary/custom/CustomDictionary.txt").split(";");
				String prePath = root;
				for (int i = 0; i < pathArray.length; ++i) {
					if (pathArray[i].startsWith(" ")) {
						pathArray[i] = prePath + pathArray[i].trim();
					} else {
						pathArray[i] = root + pathArray[i];
						int lastSplash = pathArray[i].lastIndexOf('/');
						if (lastSplash != -1) {
							prePath = pathArray[i].substring(0, lastSplash + 1);
						}
					}
				}
				CustomDictionaryPath = pathArray;
				CRFDependencyModelPath = root + p.getProperty("CRFDependencyModelPath", CRFDependencyModelPath);
				ShowTermNature = "true".equals(p.getProperty("ShowTermNature", "true"));
				String t = p.getProperty("SpiltPunctuation","");
				SpiltPunctuation = t.split("\\|\\$");
				
				String filterTerms[] = new String[]{"（依法须经批准的项目，经相关部门批准后方可开展经营活动）"};// from config
				for(int j = 0; j < filterTerms.length; ++ j){
					FilterTerms.add(HanLPMock.backupContentWithParenthese(filterTerms[j]));
				}
				/////////////////////////////////////////////////////////////////////////////////
			} catch (Exception e) {
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
				sbInfo.append("现在HanLP将尝试从").append(System.getProperties().get("user.dir")).append("读取data……");
				logger.severe("没有找到hanlp_rlp.properties，可能会导致找不到data\n" + sbInfo);
			}
		}
	}
	
	public static void test$run(){
		System.out.println(Config.CustomDictionaryPath.length);
		for(int j = 0; j < Config.CustomDictionaryPath.length; ++ j){
			System.out.println(Config.CustomDictionaryPath[j]);
		}
		System.out.println("=========test run success=========");
		String txt_1 = "中医药产业投资；中药材种植及初加工；鹿与林蛙养殖；中医药技术研发；医药制造；旅游开发；养老服务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ "从事中成药、化学药制剂、抗生素、生物制品（限诊断药品）、体外诊断试剂、321医用电子仪器设备、240/340临床检验分析仪器、345体外循环及血液处理设备、346植入材料和人工器官、364医用卫生材料及敷料、266/366医用高分子材料及制品、377介入器材批发、零售（医疗器械经营企业许可证有效期至2015年4月22日、药品经营许可证有效期至2015年4月22日，企业应在许可证有效期内开展经营活动）；以及化妆品、预包装食品（调味品、饮料）、日用品、保健食品的批发和进出口业务（食品流通许可证有效期至2013年7月11日）；医药产品、医药产品注册的咨询服务及医药领域软件开发#（依法须经批准的项目，经相关部门批准后方可开展经营活动）";
		System.out.println(HanLPMock.$core(txt_1));
	}
	
	public static void main(String args[]){
		test$run();
	}
	
	public static List<String> $core(String text$1){

        System.out.println("=====markSplit:"+Config.MARK_FOR_SPLIT+" & "+Config.MARK_FOR_PROTECTION);
		text$1 = backupContentWithParenthese(text$1);
		text$1 = replaceMarks(text$1, Config.MARK_FOR_SPLIT);

        System.out.println(text$1);
        
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
        Set<Map.Entry<String, Float>> entrySet = textRankKeyword.getRank(list).entrySet();
        List<String> result = new ArrayList<String>(entrySet.size());
        for (Map.Entry<String, Float> entry : entrySet)
        {
            result.add(restoreContentWithParenthese(entry.getKey(), Config.RESTORE_CHAR));
        }
        return result;
	}
	
	private static String replaceMarks(String content, String mark){
		for(int i = 0; i < HanLPMock.Config.SpiltPunctuation.length; ++ i){
        	if(!HanLPMock.Config.SpiltPunctuation[i].isEmpty()){
        		content = content.replaceAll(HanLPMock.Config.SpiltPunctuation[i], mark);
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
	
	public static String filterReduce(String seg){
		//双向查询，可以提高一点速度
		String temp = seg;
		if(!Config.FilterTerms.contains(seg)){
			for(String s : Config.FilterTerms){
				if(seg.contains(s)){
					seg = seg.replaceAll(s, "");
					logger.log(Level.INFO, String.format("Reduce one seg %s ==> %s",temp,seg));
					System.out.println(String.format("Reduce one seg %s ==> %s",temp,seg));
					return seg;
				}
			}
		}else{
			logger.log(Level.INFO, String.format("Filter one seg %s ==> %s",temp,seg));
			System.out.println(String.format("Filter one seg %s ==> %s",temp,seg));
			return ""; 
		}
		return seg;
	}
	
	/*public static List<Term> filter(List<Term> list){
		ArrayList<Term> collection = new ArrayList<Term>();
		for(int c = 0; c < Config.FilterTerms.length; ++ c){
			collection.add(new Term(backupContentWithParenthese(Config.FilterTerms[c]), Nature.n));
		}
		list.removeAll(collection);
		
		return list;
	}*/
	
	
}
