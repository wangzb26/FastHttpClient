package io.itit.itf.okhttp.test;

import io.itit.itf.okhttp.FastHttpClient;
import io.itit.itf.okhttp.HttpClient;
import io.itit.itf.okhttp.RequestCall;
import io.itit.itf.okhttp.Response;
import io.itit.itf.okhttp.callback.DownloadFileCallback;
import io.itit.itf.okhttp.callback.StringCallback;
import io.itit.itf.okhttp.interceptor.DownloadFileInterceptor;
import io.itit.itf.okhttp.util.FileUtil;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author icecooly
 *
 */
public class HttpClientTestCase /*extends TestCase*/{
	//
	//private static Logger logger=LoggerFactory.getLogger(HttpClientTestCase.class);

	public static void main(String[] args) throws Exception {
		HttpClientTestCase a = new HttpClientTestCase();
		a.testCookie();
	}
	//
	/**
	 * 同步Get请求(访问百度首页,自动处理https单向认证)
	 * @throws Exception
	 */
	public void testGetSync() throws Exception{
		String resp=FastHttpClient.get().
				url("https://www.baidu.com").
				build().
				execute().string();
		System.out.println(resp);
//		logger.info(resp);
	}
	
	/**
	 * 异步Get请求(访问百度首页)
	 * @throws InterruptedException
	 */
	public void testGetAsync() throws InterruptedException{
		FastHttpClient.get().url("https://www.baidu.com").build().
		executeAsync(new StringCallback() {
			@Override
			public void onFailure(Call call, Exception e, String id) {

				logger.error(e.getMessage(),e);
			}
			@Override
			public void onSuccess(Call call, String response, String id) {
				logger.info("response:{}",response);
			}
		});
		Thread.sleep(3000);
	}
	
	/**
	 * 百度搜索关键字'微信机器人'
	 * @throws Exception
	 */
	public void testBaiduSearch() throws Exception{
		String html = FastHttpClient.get().
				url("http://www.baidu.com/s").
				addParams("wd", "微信机器人").
				addParams("tn", "baidu").
				build().
				execute().
				string();
		System.out.println(html);
	}
	//
	@SuppressWarnings("unused")
	private static class ObjectParam{
		public String wd;
		public String tn;
	}
	public void testObjectParam() throws Exception{
		ObjectParam param=new ObjectParam();
		param.wd="微信机器人";
		param.tn="baidu";
		String resp=FastHttpClient.get().
				url("http://www.baidu.com/s").
				addParams(param).
				build().
				execute().string();
		System.out.println(resp);
	}
	
	/**
	 * 异步下载一张百度图片，有下载进度,保存为tmp.jpg
	 * @throws InterruptedException
	 */
	public void testAsyncDownloadFile() throws InterruptedException{
		String savePath="tmp.jpg";
		String imageUrl="http://e.hiphotos.baidu.com/image/pic/item/faedab64034f78f0b31a05a671310a55b3191c55.jpg";
		FastHttpClient.newBuilder().addNetworkInterceptor(new DownloadFileInterceptor(){
			@Override
			public void updateProgress(long downloadLenth, long totalLength, boolean isFinish) {
				int percent=0;
				if(totalLength>0) {
					percent=(int) (downloadLenth*100/totalLength);
				}
				//logger.info("updateProgress downloadLenth:"+downloadLenth+
				//		" totalLength:"+totalLength+" percent:"+percent+"% isFinish:"+isFinish);
			}
		}).
		connectTimeout(0, TimeUnit.SECONDS).
		readTimeout(0, TimeUnit.SECONDS).
		writeTimeout(0, TimeUnit.SECONDS).
		build().
		get().
		id("1000").
		tag("download big file").
		url(imageUrl).
		build().
		executeAsync(new DownloadFileCallback(savePath) {//save file to /tmp/tmp.jpg
				@Override
				public void onFailure(Call call, Exception e, String id) {
					logger.error("onFailure id:{}",id);
					logger.error(e.getMessage(),e);
				}
				@Override
				public void onSuccess(Call call, File file, String id) {
					logger.info("filePath:"+file.getAbsolutePath()+",id:{}",id);
				}
				@Override
				public void onSuccess(Call call, InputStream fileStream, String id) {
					logger.info("onSuccessWithInputStream id:{}",id);
				}
		});
		Thread.sleep(50000000);
	}
	
	/**
	 * 同步下载文件
	 * @throws Exception
	 */
	public void testSyncDownloadFile() throws Exception{
		String savePath="tmp.jpg";
		String imageUrl="http://e.hiphotos.baidu.com/image/pic/item/faedab64034f78f0b31a05a671310a55b3191c55.jpg";
		InputStream is=FastHttpClient.get().url(imageUrl).build().execute().byteStream();
		FileUtil.saveContent(is, new File(savePath));
	}
	
	/**
	 * 上传文件(支持多个文件同时上传)
	 * @throws Exception
	 */
	public void testUploadFile() throws Exception{
		byte[] imageContent=FileUtil.getBytes("/tmp/logo.jpg");
		Response response = FastHttpClient.newBuilder().
				connectTimeout(10, TimeUnit.SECONDS).
				build().
				post().
				url("上传地址").
				addFile("file", "logo.jpg",imageContent).
				build().
				execute();
		System.out.println(response.body().string());
	}
	
	/**
	 * 上传文件(通过文件流)
	 * @throws Exception
	 */
	public void testUploadFileWithStream() throws Exception{
		InputStream is=new FileInputStream("/tmp/logo.jpg");
		Response response = FastHttpClient.newBuilder().
				connectTimeout(10, TimeUnit.SECONDS).
				build().
				post().
				url("上传地址").
				addFile("file", "logo.jpg",is).
				build().
				execute();
		System.out.println(response.body().string());
	}
	
	/**
	 * 设置网络代理
	 * @throws Exception
	 */
	public void testProxy() throws Exception{
		Proxy proxy = new Proxy(Proxy.Type.SOCKS,new InetSocketAddress("127.0.0.1", 1080));
		Authenticator.setDefault(new Authenticator(){//如果没有设置账号密码，则可以注释掉这块
	         private PasswordAuthentication authentication = 
	         		new PasswordAuthentication("username","password".toCharArray());
	         @Override
	         protected PasswordAuthentication getPasswordAuthentication(){
	             return authentication;
	         }
	     });
		Response response = FastHttpClient.
				newBuilder().
				proxy(proxy).
				build().
				get().
				url("http://ip111.cn/").
				build().
				execute();
		System.out.println(response.string());
	}
	
	/**
	 * 设置http头部信息
	 * @throws Exception
	 */
	public void testAddHeader() throws Exception{
		String url="https://www.baidu.com";
		Response response=FastHttpClient.
				get().
				addHeader("Referer","http://news.baidu.com/").
				addHeader("cookie", "uin=test;skey=111111;").
				url(url).
				build().
				execute();
		System.out.println(response.string());
	}
	//
	public void testSSLContext() throws Exception {
		SSLContext sslContext=null;//TODO
		String url="";
		Response response=FastHttpClient.newBuilder().
				sslContext(sslContext).build().
					get().
					url(url).
					build().
					execute();
		System.out.println(response.string());
	}
	//
	private class LocalCookieJar implements CookieJar{
	    List<Cookie> cookies;
	    @Override
	    public List<Cookie> loadForRequest(HttpUrl arg0) {
	         if (cookies != null) {
	                return cookies;
	         }
	         return new ArrayList<Cookie>();
	    }
	    @Override
	    public void saveFromResponse(HttpUrl arg0, List<Cookie> cookies) {
	        this.cookies = cookies;
			for (Cookie item : cookies) {
				System.out.println(item.name());
				System.out.println(item.value());
			}
			System.out.println("saveFromResponse" + cookies);
	    }
	}
	//自动携带Cookie进行请求
	public void testCookie() throws Exception {
		LocalCookieJar cookie=new LocalCookieJar();
		HttpClient client=FastHttpClient.newBuilder()
				.connectTimeout(5, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS)
				.writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(false) //禁制OkHttp的重定向操作，我们自己处理重定向
                .followSslRedirects(false)
                .cookieJar(cookie)   //为OkHttp设置自动携带Cookie的功能
                .build();
		//String url="http://mb.kingbom.com/mkapp/listPubInfo.shtml";
		String url="http://192.168.3.21/mkapp/app/getSignKey.shtml";
		Response response = client.post().addHeader("Referer","https://www.baidu.com/").
			url(url).
			build().
			execute();
		System.out.println(cookie.cookies);
		System.out.println(response.toString());
		System.out.println( response.body().string());


		HttpClient client2=FastHttpClient.newBuilder()
				.followRedirects(false) //禁制OkHttp的重定向操作，我们自己处理重定向
				.followSslRedirects(false)
				.cookieJar(cookie)   //为OkHttp设置自动携带Cookie的功能
				.build();
		String url2="http://mb.kingbom.com/mkapp/listPubInfo.shtml";
		Response response2 = client.post().addHeader("Referer","https://www.baidu.com/").
				url(url2).
				build().
				execute();
		System.out.println(cookie.cookies);
		System.out.println(response2.body().string());

		HttpClient client3=FastHttpClient.newBuilder()
				.followRedirects(false) //禁制OkHttp的重定向操作，我们自己处理重定向
				.followSslRedirects(false)
				.cookieJar(cookie)   //为OkHttp设置自动携带Cookie的功能
				.build();
		String url3="http://192.168.3.21/mkapp/app/getSignKey.shtml";
		client3.post().addHeader("Referer","https://www.baidu.com/").
				url(url3).
				build().
				executeAsync(new StringCallback() {
					@Override
					public void onFailure(Call call, Exception e, String id) {

						logger.error(e.getMessage(),e);
					}
					@Override
					public void onSuccess(Call call, String response, String id) {
						logger.info("response:{}",response);
					}
				});
		Thread.sleep(3000);
	}
	//
	public void testXForwardedFor() throws Exception{
		String url="https://www.aex.com/";
		Response response=FastHttpClient.
				get().
				addHeader("X-Forwarded-For","234.45.124.12").
				url(url).
				build().
				execute();
		System.out.println(response.string());
	}
	//
	//
	public void testPut() throws Exception{
		String url="https://www.aex.com/";
		Response response=FastHttpClient.
				put().
				addHeader("X-Forwarded-For","234.45.124.12").
				url(url).
				build().
				execute();
		System.out.println(response.string());
	}
	//
	public void testSetContentType() throws Exception{
		String url="https://wx.qq.com";
		Response response=FastHttpClient.
				post().
				addHeader("Content-Type","application/json").
				body("{\"username\":\"test\",\"password\":\"111111\"}").
				url(url).
				build().
				execute();
		System.out.println(response.string());
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testCancel() throws Exception{
		RequestCall call=FastHttpClient.get().
				url("https://www.baidu.com").
				build();
		Response response=call.execute();
		call.cancel();
		System.out.println(response.string());
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testCancelByTag() throws Exception{
		String tag="baidu";
		RequestCall call=FastHttpClient.get().
				url("https://www.baidu.com").
				tag(tag).
				build();
		call.executeAsync(new StringCallback() {
			@Override
			public void onSuccess(Call call, String response,String id) {
				logger.info("onSuccess response:{} id:{} ",response,id);
			}
		});
		FastHttpClient.cancel(tag);
		Thread.sleep(5000);
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testCancelAll() throws Exception{
		for(int i=1;i<=10;i++) {
			String tag="baidu"+i;
			RequestCall call=FastHttpClient.get().
					url("https://www.baidu.com").
					tag(tag).
					build();
			call.executeAsync(new StringCallback() {
				@Override
				public void onSuccess(Call call, String response, String id) {
					logger.info("onSuccess response:{} id:{} ",response,id);
				}
			});
		}
		FastHttpClient.cancelAll();
		Thread.sleep(5000);
	}
}
