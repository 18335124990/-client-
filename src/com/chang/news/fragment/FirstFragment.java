package com.chang.news.fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chang.news.ClientNewsActivity;
import com.chang.news.adapter.NewsAdapter;
import com.chang.news.adapter.NoticeAdapter;
import com.chang.news.model.NewsBeam;
import com.chang.news.model.NoticeBeam;
import com.chang.news.util.HttpUtil;
import com.chang.news.util.RefreshableView;
import com.chang.news.util.RefreshableView.PullToRefreshListener;
import com.imooc.tab03.R;

public class FirstFragment extends Fragment implements OnItemClickListener {
	String urlPath;
	String type;
	
	ProgressBar progressBar;
	RefreshableView refreshableView;
	ListView listView;

	NoticeAdapter adapter;
	View view;

	boolean mHasLoadedOnce = false;

	int currPageNo = 1;
	
	NewAsyncTask asyncTask;

	List<NoticeBeam> noticeList;

	Handler handler = new Handler();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		initUrlPath();
	}

	protected void initUrlPath() {
		// TODO Auto-generated method stub
		urlPath = "http://192.168.207.26:8080/HongTaiNewsService/HongTaiNoticeServlet?pageNo=";
		type = "最要通知";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.layout_first, container, false);
		progressBar = (ProgressBar) view.findViewById(R.id.load_bar);
		refreshableView = (RefreshableView) view
				.findViewById(R.id.refreshable_view);
		listView = (ListView) view.findViewById(R.id.list_view);
		noticeList = new ArrayList<NoticeBeam>();

		this.listView.setOnItemClickListener(this);
		
		asyncTask = new NewAsyncTask();
		asyncTask.execute("" + currPageNo);

		refreshableView.setOnRefreshListener(new PullToRefreshListener() {
			@Override
			public void onRefresh() {

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				List<NoticeBeam> tempList = asyncTask.getJsonData("-1");
				
				if (tempList == null || tempList.size() == 0) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getActivity(), "暂无新消息",
									Toast.LENGTH_SHORT).show();
						}
					});
				} else {
					NoticeBeam currDate = noticeList.get(0);
					List<NoticeBeam> mList = new ArrayList<NoticeBeam>();
					for (NoticeBeam newsBeam : tempList) {
						if (currDate.equals(newsBeam.getUrl())) {
							break;
						} else {
							mList.add(newsBeam);
						}
					}

					noticeList.addAll(0, mList);
					handler.post(new Runnable() {
						@Override
						public void run() {
							adapter.notifyDataSetChanged();
						}
					});
				}
				
				refreshableView.finishRefreshing();
			}

			@Override
			public void onLoad() {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						currPageNo++;
						Log.d("currPageNo", "" + currPageNo);
						List<NoticeBeam> mlist =  asyncTask.getJsonData("" + currPageNo);
						if (mlist == null) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getActivity(), "已无更多消息",
											Toast.LENGTH_SHORT).show();
								}

							});
						} else {
							noticeList.addAll(mlist);
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								adapter.notifyDataSetChanged();
								refreshableView.loadComplete();
							}

						});
					}
				}).start();
			}
		}, 0);

		return view;
	}

	class NewAsyncTask extends AsyncTask<String, Void, List<NoticeBeam>> {
		// ProgressDialog progressDialog;
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			if (!HttpUtil.isNetworkStatusOK(getActivity())) {
				progressBar.setVisibility(View.VISIBLE);
			} else {
				Toast.makeText(getActivity(), "当前网络不可用！",
						Toast.LENGTH_SHORT).show();
				this.cancel(true);
			}

		}

		@Override
		protected List<NoticeBeam> doInBackground(String... params) {
			return getJsonData(params[0]);
		}

		@Override
		protected void onPostExecute(List<NoticeBeam> newsBeams) {
			super.onPostExecute(newsBeams);
			noticeList = newsBeams;
			adapter = new NoticeAdapter(getActivity(), noticeList);
			listView.setAdapter(adapter);
			progressBar.setVisibility(View.GONE);
		}

		/**
		 * 从 URL 中获取数据
		 * 
		 * @param url
		 * @return
		 */
		public List<NoticeBeam> getJsonData(String pageNo) {
			String httpUrl = urlPath + pageNo;
			List<NoticeBeam> tempList = null;
			try {
				String jsonString = request(httpUrl);
				
				JSONArray jsonArray;
				JSONObject jsonObject;
				NoticeBeam noticeBeam;
				jsonArray = new JSONArray(jsonString);
				tempList = new ArrayList<NoticeBeam>();
				for (int i = 0; i < jsonArray.length(); i++) {
					jsonObject = (JSONObject)jsonArray.get(i);
					noticeBeam = new NoticeBeam();
					noticeBeam.setTitle(jsonObject.getString("title"));
					noticeBeam.setTime(jsonObject.getString("time"));
					noticeBeam.setUrl(jsonObject.getString("url"));
					tempList.add(noticeBeam);
				}

			} catch (JSONException e) {
				return null;
			}
			return tempList;
		}


		/**
		 * @param urlAll
		 *            :请求接口
		 * @param httpArg
		 *            :参数
		 * @return 返回结果
		 */
		public String request(String httpUrl) {
			BufferedReader reader = null;
			String result = null;
			StringBuffer sbf = new StringBuffer();
			try {
				URL url = new URL(httpUrl);
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				connection.setRequestMethod("GET");
				connection.connect();
				InputStream is = connection.getInputStream();
				reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String strRead = null;
				while ((strRead = reader.readLine()) != null) {
					sbf.append(strRead);
					sbf.append("\r\n");
				}
				reader.close();
				result = sbf.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		
		NoticeBeam temp = noticeList.get(position);
		
		Intent intent= new Intent(getActivity(),ClientNewsActivity.class);
		NoticeBeam news = new NoticeBeam();
		news.setTitle(temp.getTitle());
		news.setTime("time");
		news.setUrl(temp.getUrl());
		intent.putExtra("news",news);
		intent.putExtra("type", type);
		startActivity(intent);
	}

}
