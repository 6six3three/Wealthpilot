package zm.zenvora.pride;
import android.app.Activity;import android.os.Bundle;import android.content.Intent;import android.net.Uri;import android.graphics.Color;import android.view.Gravity;import android.view.View;import android.view.ViewGroup;import android.webkit.*;import android.widget.*;import androidx.webkit.WebViewAssetLoader;
public class MainActivity extends Activity {
 WebView web; FrameLayout splash;
 @Override public void onCreate(Bundle b) {
 super.onCreate(b);
 FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(6,29,23));
 web=new WebView(this);web.setVisibility(View.INVISIBLE);web.setBackgroundColor(Color.rgb(6,29,23));
 WebSettings st=web.getSettings();st.setJavaScriptEnabled(true);st.setDomStorageEnabled(true);
 st.setAllowFileAccess(false);st.setAllowContentAccess(false);st.setAllowFileAccessFromFileURLs(false);st.setAllowUniversalAccessFromFileURLs(false);
 st.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);st.setGeolocationEnabled(false);
 web.setWebChromeClient(new WebChromeClient());
 WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
 web.setWebViewClient(new WebViewClient(){
 @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return loader.shouldInterceptRequest(r.getUrl());}
 @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
 Uri u=r.getUrl();if("appassets.androidplatform.net".equals(u.getHost()))return false;
 if("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme())){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){}}
 return true;
 }
 @Override public void onPageFinished(WebView v,String u){web.setVisibility(View.VISIBLE);if(splash!=null){((ViewGroup)splash.getParent()).removeView(splash);splash=null;}}
 });
 root.addView(web,new FrameLayout.LayoutParams(-1,-1));
 splash=new FrameLayout(this);splash.setBackgroundColor(Color.rgb(7,35,26));
 LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);
 ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.zenvora_brand);
 int size=(int)(136*getResources().getDisplayMetrics().density);content.addView(logo,new LinearLayout.LayoutParams(size,size));
 TextView title=new TextView(this);title.setText("ZENVORA PRIDE");title.setTextColor(Color.rgb(240,209,141));title.setTextSize(29);title.setGravity(Gravity.CENTER);content.addView(title,new LinearLayout.LayoutParams(-1,-2));
 TextView desc=new TextView(this);desc.setText("EXPLORE  ·  PROTECT  ·  PROSPER\nZAMBIA KUCHALO");desc.setTextColor(Color.WHITE);desc.setTextSize(12);desc.setGravity(Gravity.CENTER);content.addView(desc,new LinearLayout.LayoutParams(-1,-2));
 splash.addView(content,new FrameLayout.LayoutParams(-1,-1));root.addView(splash,new FrameLayout.LayoutParams(-1,-1));
 setContentView(root);web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}