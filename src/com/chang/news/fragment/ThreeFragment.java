package com.chang.news.fragment;



public class ThreeFragment extends FirstFragment
{
	@Override
	protected void initUrlPath() {
		// TODO Auto-generated method stub
		urlPath = "http://192.168.207.26:8080/HongTaiNewsService/ThreeNoticeServlet?pageNo=";
		type = "学院新闻";
	}
}
