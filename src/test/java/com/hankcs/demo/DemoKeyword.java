/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/12/7 19:25</create-date>
 *
 * <copyright file="DemoChineseNameRecoginiton.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014+ 上海林原信息科技有限公司. All Right Reserved+ http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.demo;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.HanLP.Config;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.rexen.rlp.RLP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 关键词提取
 * @author hankcs
 */
public class DemoKeyword
{
    public static void main(String[] args)
    {
        String content = "程序员(英文Programmer)是从事程序开发、维护的专业人员。" +
                "一般将程序员分为程序设计人员和程序编码人员，" +
                "但两者的界限并不非常清楚，特别是在中国。" +
                "软件从业人员分为初级程序员、高级程序员、系统" +
                "分析员和项目经理四大类。算可研究方向算可相关，算可适合某个问题，算可出现的频率，算可代码，算可卡西欧";
        String text = "算法工程师\n" +
                "算法（Algorithm）是一系列解决问题的清晰指令，也就是说，能够对一定规范的输入，在有限时间内获得所要求的输出。" +
                "如果一个算法有缺陷，或不适合于某个问题，执行这个算法将不会解决这个问题。不同的算法可能用不同的时间、" +
                "空间或效率来完成同样的任务。一个算法的优劣可以用空间复杂度与时间复杂度来衡量。算法工程师就是利用算法处理事物的人。\n" +
                "\n" +
                "1职位简介\n" +
                "算法工程师是一个非常高端的职位；\n" +
                "专业要求：计算机、电子、通信、数学等相关专业；\n" +
                "学历要求：本科及其以上的学历，大多数是硕士学历及其以上；\n" +
                "语言要求：英语要求是熟练，基本上能阅读国外专业书刊；\n" +
                "必须掌握计算机相关知识，熟练使用仿真工具MATLAB等，必须会一门编程语言。\n" +
                "\n" +
                "2研究方向\n" +
                "视频算法工程师、图像处理算法工程师、音频算法工程师 通信基带算法工程师\n" +
                "\n" +
                "3目前国内外状况\n" +
                "目前国内从事算法研究的工程师不少，但是高级算法工程师却很少，是一个非常紧缺的专业工程师。" +
                "算法工程师根据研究领域来分主要有音频/视频算法处理、图像技术方面的二维信息算法处理和通信物理层、" +
                "雷达信号处理、生物医学信号处理等领域的一维信息算法处理。\n" +
                "在计算机音视频和图形图像技术等二维信息算法处理方面目前比较先进的视频处理算法：机器视觉成为此类算法研究的核心；" +
                "另外还有2D转3D算法(2D-to-3D conversion)，去隔行算法(de-interlacing)，运动估计运动补偿算法" +
                "(Motion estimation/Motion Compensation)，去噪算法(Noise Reduction)，缩放算法(scaling)，" +
                "锐化处理算法(Sharpness)，超分辨率算法(Super Resolution),手势识别(gesture recognition),人脸识别(face recognition)。\n" +
                "在通信物理层等一维信息领域目前常用的算法：无线领域的RRM、RTT，传送领域的调制解调、信道均衡、信号检测、网络优化、信号分解等。\n" +
                "另外数据挖掘、互联网搜索算法也成为当今的热门方向。\n" +
                "算法工程师逐渐往人工智能方向发展。";
        
        String text$1 = "从事中成药、化学药制剂、抗生素、生物制品（限诊断药品）、体外诊断试剂、321医用电子仪器设备、240/340临床检验分析仪器、345体外循环及血液处理设备、346植入材料和人工器官、364医用卫生材料及敷料、266/366医用高分子材料及制品、377介入器材批发、零售（医疗器械经营企业许可证有效期至2015年4月22日、药品经营许可证有效期至2015年4月22日，企业应在许可证有效期内开展经营活动）；以及化妆品、预包装食品（调味品、饮料）、日用品、保健食品的批发和进出口业务（食品流通许可证有效期至2013年7月11日）；医药产品、医药产品注册的咨询服务及医药领域软件开发#（依法须经批准的项目，经相关部门批准后方可开展经营活动）";
        
        
        CustomDictionary.add("算可");

        
        //List<String> keywordList = HanLP.extractKeyword(content, 10);
        
        //System.out.println(keywordList);
        
        //List<String> keywordList1 = HanLP.extractPhrase(content, 10);
        
        //System.out.println(keywordList1);
        //TextRankKeyword.
        CoreStopWordDictionary.contains("程序员");
        System.out.println(HanLP.extractKeyword(content, 10));
        
        System.out.println(HanLP.extractPhrase(text$1, 10));
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // 						split and rank 						 						 					 	//
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        System.out.println(RLP.extractKeyword2(text$1, 5));
        
    }
    
    public static void getByFilter(){
    	
    }
}
