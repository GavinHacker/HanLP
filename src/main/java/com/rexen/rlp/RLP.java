package com.rexen.rlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hankcs.hanlp.algoritm.MaxHeap;
import com.hankcs.hanlp.corpus.io.IIOAdapter;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.hankcs.hanlp.utility.Predefine;
import com.hankcs.hanlp.utility.TimeStampUtil;

import static com.hankcs.hanlp.utility.Predefine.logger;

class RLogHandler extends Formatter {
	@Override
	public String format(LogRecord record) {
		return record.getLevel() + " *** " + record.getMessage() + "\n";
	}
}

public class RLP {
	
	//private static Logger logger = Logger.getLogger("RLP");
	
	public static final class Config {
		
		public static boolean PrintFilterAndReduce = false;
		
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
		
		public static HashMap<String, Integer> FilterAndReduceFlage = new LinkedHashMap<String, Integer>();

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
					Config.FilterAndReduceFlage.put(s.contains("*")||s.contains(".")||s.contains("|")||s.contains("^") ? s : backupContentWithParenthese(s), stopPhrase2FlagMap.get(s));
				}
				
				String t = p.getProperty("SpiltPunctuation","");
				SpiltPunctuation = t.split("\\|\\$");
				String pt = p.getProperty("PrintFilterAndReduce", "false");
				PrintFilterAndReduce = Boolean.parseBoolean(pt);
				/////////////////////////////////////////////////////////////////////////////////
				/*ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel(Level.FINE);
				logger.addHandler(consoleHandler);
				FileHandler fileHandler = new FileHandler("/Users/gavin/workspace_machinelearn/HanLP/HanLP/rlp.log");
				fileHandler.setLevel(Level.FINE);
				fileHandler.setFormatter(new RLogHander());
				logger.addHandler(fileHandler);*/
				//System.setProperty("java.util.logging.config.file", "logging.properties");
				//logger = Logger.getLogger("RLP");
				
				logger = Logger.getLogger("RLP");
				FileHandler fileHandler = new FileHandler(TimeStampUtil.dateToString(new Date(), "yyyyMMddHHmmss")+".log");
				fileHandler.setLevel(Level.FINE);
				fileHandler.setFormatter(new RLogHandler());
				logger.addHandler(fileHandler);
				logger.getParent().removeHandler(logger.getParent().getHandlers()[0]);
				
				for(Handler h : logger.getHandlers()){
					logger.info(h.getClass().getName());
				}
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
		Pattern p = Pattern.compile("(?<=\\(|（)[^\\)|）]+");//|(?<=（)[^）]+|(?<=\\()[^）]+|(?<=（)[^\\)]+
		Matcher m = p.matcher(content);
		ArrayList<String> strs = new ArrayList<String>();
		
        while (m.find()) {
            strs.add(m.group());            
        }
        for(String s : strs){
        	content = content.replace(s, replaceMarks(s, Config.MARK_FOR_PROTECTION));
        }
        return content;
	}
	
	public static String restoreContentWithParenthese(String content, String mark){
		content = content.replace(Config.MARK_FOR_PROTECTION, mark);
		return content;
	}
	
	public static String filterReduce(String seg){
		return filterReduce(seg, true);
	}
	
	public static String filterReduce(String seg, boolean withStatusLog) {
		boolean subFinish = false;
		String temp = seg;
		
		int index_1 = 0;
		int index_2 = 0;
		int len = 0;
		String rS = "";
		final String REGEX_EX_FUNCTION_LESS_THAN = "func_less_than_len__"; 
		
		for (String s : Config.FilterAndReduceFlage.keySet()) {
			len = 0;
			if ((Config.FilterAndReduceFlage.get(s) & Config.Regex) == Config.Regex) {
				rS = s;
				if(s.contains(REGEX_EX_FUNCTION_LESS_THAN)){
					////////////////////////////////////////
					index_1 = s.indexOf(REGEX_EX_FUNCTION_LESS_THAN);
					if(index_1 >= 0){
						index_2 = s.lastIndexOf("__");
						len = Integer.parseInt(s.substring(index_1+REGEX_EX_FUNCTION_LESS_THAN.length(), index_2));
						rS = s.substring(0, s.length()-s.substring(index_1).length());
					}
					////////////////////////////////////////
				}
				Pattern p2 = Pattern.compile(rS);
				Matcher m2 = p2.matcher(seg);
				while (m2.find()) {   //from if
					String group = m2.group();
					if(group.length() < len){
						continue;
					}
					if (!subFinish && (Config.FilterAndReduceFlage.get(s) & Config.FilterFlag) == Config.FilterFlag) {
						seg = "";
						if(withStatusLog){
							logger.log(Level.INFO, String.format("Regex Filter one seg %s --- %s ==> %s", temp, s, seg));
							//System.out.println(String.format("Regex Filter one seg %s --- %s ==> %s", temp, s, seg));
						}
						subFinish = true;
					}
					
					if ((Config.FilterAndReduceFlage.get(s) & Config.ReduceFlag) == Config.ReduceFlag) {
						seg = seg.replace(group, "");
						if(withStatusLog){
							logger.log(Level.INFO, String.format("Regex Reduce one seg %s --- %s ==> %s", temp, s, seg));
							//System.out.println(String.format("Regex Reduce one seg %s --- %s ==> %s", temp, s, seg));
						}
					}
					
					if (!subFinish && (Config.FilterAndReduceFlage.get(s) & Config.ClauseFilterFlag) == Config.ClauseFilterFlag) {
						seg = "";
						if(withStatusLog){
							logger.log(Level.INFO, String.format("Regex Clause filter one seg %s --- %s ==> %s", temp, s, seg));
							//System.out.println(String.format("Regex Clause filter one seg %s --- %s ==> %s", temp, s, seg));
						}
						subFinish = true;
					}
				}
			}
		}
		// 双向查询
		if (Config.FilterAndReduceFlage.containsKey(seg)) {
			if ((Config.FilterAndReduceFlage.get(seg) & Config.FilterFlag) == Config.FilterFlag) {
				seg = "";
				if(withStatusLog){
					logger.log(Level.INFO, String.format("Filter one seg %s --- %s ==> %s", temp, temp, seg));
					//System.out.println(String.format("Filter one seg %s --- %s ==> %s", temp, temp, seg));
				}
				subFinish = true;
			}
		}

		if (!subFinish) {
			for (String s : Config.FilterAndReduceFlage.keySet()) {
				if ((Config.FilterAndReduceFlage.get(s) & Config.Regex) != Config.Regex && seg.contains(s) && seg.length() > s.length()) {

					if ((Config.FilterAndReduceFlage.get(s) & Config.ReduceFlag) == Config.ReduceFlag) {
						seg = seg.replace(s, "");
						if (withStatusLog) {
							logger.log(Level.INFO, String.format("Reduce one seg %s --- %s ==> %s", temp, s, seg));
							//System.out.println(String.format("Reduce one seg %s --- %s ==> %s", temp, s, seg));
						}
					}

					if (!subFinish && (Config.FilterAndReduceFlage.get(s) & Config.ClauseFilterFlag) == Config.ClauseFilterFlag) {
						seg = "";
						if(withStatusLog){
							logger.log(Level.INFO, String.format("Clause filter one seg %s --- %s ==> %s", temp, s, seg));
							//System.out.println(String.format("Clause filter one seg %s --- %s ==> %s", temp, s, seg));
						}
						subFinish = true;
					}
				}
			}
		}
		return seg;
	}
	
	public static HashMap<String, Integer> readFileByLines(String fileName) {
		HashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
		//File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader =  new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));  //new BufferedReader(new FileReader(file));
			String str = null;
			int index = 1;

			while ((str = reader.readLine()) != null) {
				String[] tempArray = str.split(":");
				result.put(tempArray[0], Integer.valueOf(tempArray[1]));
				logger.info("phrase index " + index + ": " + str);
				//System.out.println("phrase index " + index + ": " + str);
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
        	String seg = filterReduce(phraseArray[c].trim(), Config.PrintFilterAndReduce);
			if (!seg.isEmpty()) {
				Term t = new Term(seg, Nature.n);
				list.add(t);
			}
        }
        //filter(list);
        TextRankKeyword textRankKeyword = new TextRankKeyword();
        Set<Map.Entry<String, Float>> entrySet = getRankBySize(textRankKeyword.getRank(list), size).entrySet();
        //System.out.println(entrySet);
        List<String> result = new ArrayList<String>(entrySet.size());
        for (Map.Entry<String, Float> entry : entrySet)
        {
            result.add(restoreContentWithParenthese(entry.getKey(), Config.RESTORE_CHAR));
        }
        return result;
	}
	
	public static void main(String args[]){
		
		String ct = "许可、生产(米玩除外)、片剂(除大药房)、玩机(片剂除了税)、生产浓缩丸(生产不含)、除外楼、生产超级浓缩丸(不含有)";
		ct = "ddddd（除外）";
		List<String> keywordList1 = RLP.extractKeyword2(ct, 10);
		System.out.println(keywordList1);
		if(true) return;
		System.out.println("=========test run success=========");
		
		String txt_1 = "产业投资;产业投资;产业投资;产业投资;google,google,google,中医药产业投资；中药材种植及初加工；鹿与林蛙养殖；中医药技术研发；医药制造；旅游开发；养老服务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".化学药制剂、抗生素、生物制品（限诊断药品）、体外诊断试剂、吉林.产业投资;321医用电子仪器设备、google,240/340临床检验分析仪器、345体外循环及血液处理设备、346植入材料和人工器官、产业投资;364医用卫生材料及敷料、266/366医用高分子材料及制品、377介入器材批发、零售（医疗器械经营企业许可证有效期至2015年4月22日、药品经营许可证有效期至2015年4月22日，企业应在许可证有效期内开展经营活动）；以及化妆品、预包装食品（调味品、饮料）、日用品、保健食品的批发和进出口业务（食品流通许可证有效期至2013年7月11日）；医药产品、医药产品注册的咨询服务及医药领域软件开发#（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".吉林.从事中成药.google"
				+ ".商务信息咨询、投资信息咨询、企业管理信息咨询、计算机技术咨询（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".以自有资金对中医药、农副产品、旅游业、畜牧业项目进行投资。（依法须经批准的项目，经相关部门批准后方可开展经营活动）"
				+ ".中医药产业投资；中药材种植及初加工；鹿与林蛙养殖；中医药技术研发；医药制造；旅游开发；养老服务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）";
		txt_1 = txt_1 + "生产：片剂、硬胶囊剂、颗粒剂、丸剂（蜜丸、水丸、水蜜丸、浓缩丸）、合剂、糖浆剂、酒剂、茶剂、含漱剂（药品生产许可证有效期至2020年12月31日，企业应在许可证有效期内开展经营活动）。（依法须经批准的项目，经相关部门批准后方可开展经营活动）硬胶囊剂、片剂、颗粒剂、软膏剂（依法须经批准的项目，经相关部门批准后方可开展经营活动）按药品生产许可证核定的经营范围从事药品生产制造。（依法须经批准的项目，经相关部门批准后方可开展经营活动）医药研制开发及研制信息咨询X***（依法须经批准的项目，经相关部门批准后方可开展经营活动）人用合成原料药***（依法须经批准的项目，经相关部门批准后方可开展经营活动）丸剂（蜜丸、水蜜丸、水丸、浓缩丸）、原料药（人参茎叶皂甙）、硬胶囊剂、颗粒剂、片剂、散剂、合剂、小容量注射剂生产（医药生产项目异地改造，不准从事任何生产经营活动，此营业执照只保留法人主体资格一年）。（依法须经批准的项目，经相关部门批准后方可开展经营活动）地产中药材购销、生产加工。（依法须经批准的项目，经相关部门批准后方可开展经营活动）地产中药材购销、生产加工。（依法须经批准的项目，经相关部门批准后方可开展经营活动）地产中药材购销、生产加工。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西成药、药材、中药饮片、卫生材料、医药保健品（依法须经批准的项目，经相关部门批准后方可开展经营活动）小容量注射剂生产，药品研发与技术转让、技术服务；（依法须经批准的项目，经相关部门批准后方可开展经营活动）药用黄酒 酒精（依法须经批准的项目，经相关部门批准后方可开展经营活动）按生产许可证核定范围从事生产经营。（依法须经批准的项目，经相关部门批准后方可开展经营活动）人参加工、二三类中小药材收购、葡萄原汁加工、普通货运（依法须经批准的项目，经相关部门批准后方可开展经营活动）生物药品、食品、保健食品、营养食品研发。（法律、法规和国务院决定禁止的项目不得经营，依法须经批准的项目，经相关部门批准后方可开展经营活动）**片剂、硬胶囊剂、颗粒剂、合剂、糖浆剂、保健食品（中梅牌参茸王浆口服液、中梅牌天麻蜂王浆口服液）生产。（以上经营项目，法律、法规和国务院决定禁止的，不得经营；许可经营项目凭有效许可证或批准文件经营；一般经营项目可自主选择经营。）（依法须经批准的项目，经相关部门批准后方可开展经营活动）抗生素、化学药制剂、中成药、生化药品销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、硬胶囊剂、颗粒剂、合剂、糖浆剂制造、销售；农产品收购。（依法须经批准的项目，经相关部门批准后方可开展经营活动）筹建X***（依法须经批准的项目，经相关部门批准后方可开展经营活动）人参、苍术、贝母、五味子加工、本公司生产所需原材料收购（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、丸剂（蜜丸、水蜜丸、水丸、浓缩丸、糊丸）、茶剂、糖浆剂、合剂、颗粒剂、硬胶囊剂、散剂、口服溶液剂、栓剂、软膏剂、乳膏剂、凝胶剂、贴膏剂（橡胶膏剂）（药品生产许可证有效期至2020年12月31日）；保健食品生产（食品生产许可证有效期至2021年5月16日）；进出口贸易（国家法律、法规禁止的品种除外）。（依法须经批准的项目，经相关部门批准后方可开展经营活动）纸箱，纸盒，包装片材。（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产西药，中成药；保健食品(按卫生许可证许可项目经营)。（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产、销售：合剂；线上线下批发零售：食品、保健品、日用百货（依法须经批准的项目，经相关部门批准后方可开展经营活动）粉针剂（头孢菌素类）、膜剂、溶液剂（外用）、原料药（褐藻多糖硫酸酯、依达拉奉、栀子提取物、盐酸甲砜霉素甘氨酸酯）、无菌原料药（头孢匹胺、盐酸头孢甲肟、盐酸头孢吡肟、头孢西酮钠、头孢地嗪钠、盐酸头孢替安）、硬胶囊剂、片剂、颗粒剂、丸剂（水丸、浓缩丸）、小溶量注射剂、冻干粉针剂药品生产、批发、零售；经营本企业自产产品及相关技术的出口业务（国家限定公司经营或禁止出口的商品除外）；经营本企业生产、科研所需的原辅材料、机械设备、仪器仪表、零配件及相关技术的进口业务（国家限定公司经营或禁止进口的商品除外）；经营本企业的进料加工和“三来一补”业务；种养殖业。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药、化学药制剂、抗生素零售（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、硬胶囊剂、颗粒剂、合剂（含口服液）、散剂（口服、外用） 、酊剂、原料药（牡蛎钙）（以上项目有效期至2005年12月31日止）（依法须经批准的项目，经相关部门批准后方可开展经营活动）中医临床医疗技术转让、咨询、中成药、中药、日用化工产品及保健食品、饮料、生产技术研究、咨询、日用化工、自研科技产品试制、生产、销售；计算机网络信息服务。***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西原料药、制剂、保健品等医药新产品的研究开发和技术咨询、技术服务和技术转让（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药生产（经营项目有效期至2005年12月31日）（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂（含头孢菌素类）、硬胶囊剂（含头孢菌素类）、颗粒剂、丸剂（水蜜丸、水丸）生产；进出口贸易；小尾寒羊养殖及销售（依法须经批准的项目，经相关部门批准后方可开展经营活动）人参、鹿茸生产、加工销售（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产经营中成药（依法须经批准的项目，经相关部门批准后方可开展经营活动）加工、销售中成药、中药提取物、化学药制剂；人参收购、销售、粗加工；生产、销售中药饮片（直接口服饮片）、饮料（果蔬汁类及其饮料类、固体饮料类、其他饮料类）、糖果制品、水果制品（蜜饯）、茶叶及相关制品（调味茶、代用茶、速溶茶类）；生产、销售保健食品；研发、生产、销售动、植物提取物；法律、法规允许的进出口贸易。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西制剂、中药原料及保健品生产、销售***（依法须经批准的项目，经相关部门批准后方可开展经营活动）新药的研制、开发、技术协作、咨询服务、成果转让（依法须经批准的项目，经相关部门批准后方可开展经营活动）用于医药、食品、保健品研发、生产项目建设（不得从事生产经营活动，有效期至2013年6月28日）；进出口贸易（取得备案后方可经营）（依法须经批准的项目，经相关部门批准后方可开展经营活动）新医药、食品、保健品的研制开发；经济信息咨询；科技成果转让服务；市场调研；招商引资；企业IC设计X***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药、化学药制剂、抗生素、生化药品、保健食品、医疗器械销售（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西药（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药制造***    ***（依法须经批准的项目，经相关部门批准后方可开展经营活动）合剂（含口服液）、片剂、硬胶囊剂、颗粒剂、丸剂（水丸）；胶囊剂（头孢菌素类）生产、销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西成药 化学消毒剂***（依法须经批准的项目，经相关部门批准后方可开展经营活动）天然药物（中药，生化药），西药的研制，开发，咨询，技术转让，技术服务，中成药（口服液）生产。***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中药材收购 加工 预包装产品 散装产品  批发 零售（法律、法规和国务院决定禁止的项目不得经营；许可经营项目凭有效许可证或批准文件经营；一般经营项目可自主选择经营）（依法须经批准的项目，经相关部门批准后方可开展经营活动）人参、西洋参、中药材晾晒、销售；鹿产品、蛤蟆油、收购、销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）按药品生产许可证核定的范围从事生产经营。（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、硬胶囊剂、颗粒剂、丸剂、合剂生产；经营本企业自产产品及相关技术的出口业务（国家限定公司经营或禁止出口的商品除外）；经营本企业生产、科研所需的原辅材料、机械设备、仪器仪表、零配件及相关技术的进口业务（国家限定公司经营或禁止进口的商品除外）；经营本企业的进料加工“三来一补”业务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）许可经营范围：无；一般经营范围：药品研发（不含生产、销售）（依法须经批准的项目，经相关部门批准后方可开展经营活动）合剂、搽剂、口服乳剂、散剂、片剂、硬胶囊剂、颗粒剂、丸剂（水蜜丸、水丸）生产，中药材种植、购销。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西成药（依法须经批准的项目，经相关部门批准后方可开展经营活动）生物医药和产品、医用材料、试剂、药品的研究开发X***（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂（含青霉素类）、硬胶囊剂（含青霉素类）、颗粒剂、散剂、酒剂、丸剂（蜜丸、水蜜丸、水丸、浓缩丸）、合剂、糖浆剂、乳膏剂、软膏剂、栓剂、凝胶剂、乳膏剂（含激素类）、软膏剂（含激素类）制造；II类6864卫生材料及敷料的生产。（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、硬胶囊剂、软胶囊剂、合剂、丸剂（蜜丸、水丸、水蜜丸、浓缩丸）、煎膏剂、软膏剂、酒剂、颗粒剂（含中药前处理及提取）；口服液生产、销售；土特产品、中药材（国家限定的品种除外）采购、仓储。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西成药开发、技术咨询（依法须经批准的项目，经相关部门批准后方可开展经营活动）化学药制剂、中成 保健品、中药材加工（批发零售本公司生产的产品）。（国家禁止或限制经营的项目除外；涉及许可项目凭许可证或批准文件在有效期内经营。）（依法须经批准的项目，经相关部门批准后方可开展经营活动）颗粒剂 硬胶囊剂 片剂 合剂 散剂制造 加工***（依法须经批准的项目，经相关部门批准后方可开展经营活动）批发、零售、加工中西药制剂，农副产品收购。（依法须经批准的项目，经相关部门批准后方可开展经营活动）食品用、药用胶囊、药用辅料生产及销售；胶囊生产设备租赁及销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）人参收购、晾晒、销售；土特产品收购、加工；中药材种植；进出口业务。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药、化学药制剂、抗生素、生化药品、保健食品、工艺品、日用百货、化妆品、保健用品、卫生用品、消毒用品（不含危险化学品）零售；二类医疗器械（除体外诊断试剂）零售。（国家禁止或限制项除外；依法须经批准的项目，经相关部门批准后方可开展经营活动。）化学药制剂、抗生素、中成药、中药饮片、生化药品、保健食品、医疗器械销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产颗粒剂#医药制造#片剂、胶囊剂、颗粒剂、冻干粉针剂、小容量注射剂、滴眼剂生产（许可证有效期至2005年12月31日）及新产品的研究与开发X（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产销售：医药制品（化学药剂、抗生素、生物制品、中成药、保健品、中药饮片、卫生材料及医疗器械）凭许可证经营（依法须经批准的项目，经相关部门批准后方可开展经营活动）对制药业投资、对证券业投资＊＊＊（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、硬胶囊剂、颗粒剂、贴膏剂、粉剂生产、销售（依法须经批准的项目，经相关部门批准后方可开展经营活动）中药片散剂.溶液剂合剂（含口服液）、片剂、硬胶囊剂、颗粒剂、丸剂（水丸）、胶囊剂（头孢菌素类）生产、销售***（依法须经批准的项目，经相关部门批准后方可开展经营活动）硬胶囊剂、片剂、颗粒剂、冻干粉针剂、小容量注射剂、滴眼剂生产（许可证有效期至2010年12月31日）及新产品的研究与开发*药品的研究、开发（以上各项法律、行政法规、国务院规定禁止的不得经营；需经专项审批的项目未经批准之前不得经营）满医药研发、生产加工、销售***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药、西药生产；中药饮片加工；保健食品生产；中药材种植；畜牧养殖；玻璃制品、铝制品生产；经营本企业自产产品及技术的出口业务和本企业所需的机械设备、零配件、原辅材料及技术的进口业务、但国家限定公司经营或禁止进出口的商品及技术除外***（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产口服液药品及其原料药、中西药制剂及部分原料药#（依法须经批准的项目，经相关部门批准后方可开展经营活动）业务联系#筹建**医药#筹建#化学药品制剂、中成药加工；经营本企业自产产品及相关技术的出口业务（国家限定公司经营或禁止出口的商品除外）；经营本企业生产、科研所需的原辅材料、机械设备、仪器仪表、零配件及相关技术的进口业务（国家限定公司经营或禁止进口的商品除外）；经营本企业的进料加工和“三来一补”业务；限于吉林省内收购人工驯养鹿、林蛙产品；须在国家定点单位采购麝香、羚羊、岩羊角、猴枣、熊骨、龟甲、玳瑁片、穿山甲、蟾酥、乌梢蛇、土鳖虫、僵蚕、蝎子（许可证有效期至2009年3月）***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药、化学药制剂、抗生素、生化药品、、中药饮片、保健食品、医疗器械销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）血液制剂***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中药材切片加工，农副产品收购、销售，保健茶、预包装食品兼散装食品销售（以上经营项目,法律、法规和国务院决定禁止的，不得经营； 许可经营项目凭有效许可证或批准文件经营；一般经营项目可自主选择经营）*生产中药怀炉和普通怀炉#抗生素、化学药制剂、中成药、中药饮片、生物制品零售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）种植花旗参.加工花旗参系列产品#片剂、硬胶囊剂、颗粒剂、丸剂（蜜丸、水蜜丸、水丸、浓缩丸）、合剂（含口服液）、酒剂、酊剂、糖浆剂、软胶囊剂、中药饮片生产；进出口贸易（国家法律、法规禁止的品种除外）***（依法须经批准的项目，经相关部门批准后方可开展经营活动）小容量注射剂、大容量注射剂、原料药（丹参酮IIA磺酸钠、硫普罗宁、甘草酸单胺盐S）、无菌原料药(炎琥宁、过氧化碳酰胺）（含中药前处理及提取）（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、颗粒剂、硬胶囊剂、小容量注射剂、冻干粉针剂、中药饮片（净制、切制、炒制）X（许可证有效期至2010年12月31日）***（依法须经批准的项目，经相关部门批准后方可开展经营活动）（三级）处方药、非处方药（中成药、化学药制剂、抗生素），中药饮片，生物制品（除疫苗）；血液制品除外；零售221医用电子仪器设备 223医用超声仪器及有关设备 226物理治疗及康复设备 227中医器械 241医用化验和基础设备器具 254手术室、急救室、诊疗室设备及器具 256病房护理设备及器具 264医用卫生材料及敷料 266医用高分子材料及制品 315注射穿刺器械 322医用光学器具、仪器及内窥镜设备 366医用高分子材料及制品 一次性使用无菌医疗器械（零售）；保健食品零售连锁经营；消毒用品经销（依法须经批准的项目，经相关部门批准后方可开展经营活动）胶囊剂、片剂、颗粒剂生产销售（依法须经批准的项目，经相关部门批准后方可开展经营活动）*前期筹建*（依法须经批准的项目，经相关部门批准后方可开展经营活动）野山参、人参、林下参晾晒、销售***（依法须经批准的项目，经相关部门批准后方可开展经营活动）中药材饮片、中药材活性成分浓缩液、健康饮品及其制品的生产和销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）热原吸附剂.吸附过滤设备及有关器木生化药品.血液制品及来料另装#片剂、硬胶囊剂、颗粒剂、医用材料、降解材料、空心胶囊、食品生产、销售（依法须经批准的项目，经相关部门批准后方可开展经营活动）粉散剂  兽药原料药 （依法须经批准的项目，经相关部门批准后方可开展经营活动）合剂、颗粒剂、硬胶囊剂、片剂、小容量注射剂、口服溶液剂、糖浆剂、栓剂、丸剂（浓缩丸、糊丸）；中药材的种植、研究；中药饮片、药品、保健品的研发、销售；医疗器械的研究、开发；医疗项目投资。（依法须经批准的项目，经相关部门批准后方可开展经营活动）。生产软膏制剂*筹建*大容量注射剂(按许可证核定的经营范围和期限经营)联络业务研究、开发生物制药、化学制药、制剂、中药、基因工程、生物农药、保健品*各种剂型的中西成药、新药的研制与开发***（依法须经批准的项目，经相关部门批准后方可开展经营活动）联络业务片剂、硬胶囊剂、颗粒剂、软膏剂、小容量注射剂（药品生产许可证有效期至2010年12月31日）；进出口贸易（国家法律法规禁止的项目除外）X***（依法须经批准的项目，经相关部门批准后方可开展经营活动）生产西药制剂生产黑加伦籽油.黑加伦口服液及其系列保健品和研究开发新产品#房地产开发、室内建筑装璜#（依法须经批准的项目，经相关部门批准后方可开展经营活动）中成药、化学药制剂、抗生素（药品经营许可证有效期至2020年5月31）。（依法须经批准的项目，经相关部门批准后方可开展经营活动）。片剂、硬胶囊剂、颗粒剂生产。（依法须经批准的项目，经相关部门批准后方可开展经营活动）联络业务*医用氧、工业氧、氩气、乙炔、二氧化碳等气体 液态气体 无缝气体钢瓶检测 危险品道路运输（依法须经批准的项目，经相关部门批准后方可开展经营活动）片剂、硬胶囊剂、颗粒剂（药品生产许可证有效期至2005年12月31日止）***（依法须经批准的项目，经相关部门批准后方可开展经营活动）丸剂（蜜丸、水蜜丸、水丸、浓缩丸）、片剂（含外用）、硬胶囊剂、颗粒剂、栓剂、合剂、口服液、口服溶液剂、糖浆剂、冻干粉针剂（抗肿瘤药）、原料药【奈达铂（抗肿瘤药）】。（依法须经批准的项目，经相关部门批准后方可开展经营活动）中西药、生物制剂、生物制品的研发；保健用品的研发、生产、销售；一、二类医疗器械生产、销售；中药材种植、研发；卫生用品、消杀类用品（消毒类产品）的研发、生产、销售；保健食品、食品研制、研发、生产及销售；饮料、口服液、保健酒的研发、生产及销售；化妆品的研发、生产及销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）丸剂(蜜丸 水丸 浓缩丸) 硬胶囊剂 散剂 片剂 颗粒剂 酊剂(含外用) 糖浆剂 以及上述产品的进出口业务（依法须经批准的项目，经相关部门批准后方可开展经营活动）基因重组制品（注射用重组葡激酶）生产；化工产品（国家限制产品除外）、经营本企业自产医药产品出口业务（国家组织统一联合经营的16种出口商品除外）；本企业生产科研所需的原辅材料、机械设备、仪器仪表及零配件的进口业务（国家实行核定公司经营的14种进口产品除外）***（依法须经批准的项目，经相关部门批准后方可开展经营活动）联络业务中成药、中药饮片、化学药制剂、抗生素、生化药品、保健食品（按卫生许可证核定的经营范围）销售。（依法须经批准的项目，经相关部门批准后方可开展经营活动）";
		txt_1 +=  "（依法须经批准的项目、经相关部门批准后方可开展经营活动）人参;";
		txt_1 += "（依法须经批准的项目，经相关部门批准后方可开展经营活动)人参1,";
		txt_1 += "(依法须经批准的项目，经相关部门批准后方可开展经营活动)人参2,";
		txt_1 += "(依法须经批准的项目，经相关部门批准后方可开展经营活动）人参3";
		txt_1 = "机械加工X***（依法须经批准的项目、经相关部门批准后方可开展经营活动）影视古装艺术照，销售";
		
		System.out.println("\r\n\r\n"+RLP.extractKeyword2(txt_1, 100));
	}
	
}
