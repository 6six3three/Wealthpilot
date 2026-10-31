package zm.zenvora.pride;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.biometrics.BiometricPrompt;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
  private WebView webView;
  private FrameLayout splash;
  private static final String BASE="https://appassets.androidplatform.net";
  private static final String ALIAS="zenvora.owner.biometric.v1";
  private static final String PREFS="zenvora.device.biometric.v1";
  private static final String P_WRAPPED="ciphertext";
  private static final String P_IV="iv";
  private static final String P_SALT="vault_salt";
  private static final int VOICE_REQ=7782;
  private boolean busy=false;
  private SharedPreferences store(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
  private void callJS(String method, String value){
    // Invoke only hard-coded entry points on our bundled offline page; escape arbitrary strings.
    if(webView==null)return;
    String url=webView.getUrl();
    if(url==null || !url.equals(BASE+"/assets/index.html"))return;
    webView.evaluateJavascript("if(window.ZenvoraNativeLogin){window.ZenvoraNativeLogin."+method+"("+JSONObject.quote(value)+");}",null);
  }
  private void status(String message){runOnUiThread(()->callJS("onStatus",message));}
  private SecretKey currentKey() throws Exception{
    KeyStore keystore=KeyStore.getInstance("AndroidKeyStore");keystore.load(null);
    return (SecretKey)keystore.getKey(ALIAS,null);
  }
  private SecretKey newKey() throws Exception{
    KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    KeyGenParameterSpec.Builder spec=new KeyGenParameterSpec.Builder(ALIAS,
      KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setRandomizedEncryptionRequired(true)
      .setUserAuthenticationRequired(true)
      .setInvalidatedByBiometricEnrollment(true);
    if(Build.VERSION.SDK_INT>=30){
      spec.setUserAuthenticationParameters(0,KeyProperties.AUTH_BIOMETRIC_STRONG);
    } else {
      spec.setUserAuthenticationValidityDurationSeconds(-1);
    }
    generator.init(spec.build());return generator.generateKey();
  }
  private void wipe(){
    store().edit().clear().apply();
    try{KeyStore keyStore=KeyStore.getInstance("AndroidKeyStore");keyStore.load(null);keyStore.deleteEntry(ALIAS);}catch(Exception ignored){}
  }
  private void prompt(final Cipher cipher, final String mode, final String password, final String salt) throws Exception{
    if(Build.VERSION.SDK_INT<28){status("Device biometric quick unlock requires Android 9 or later. Use your passphrase.");busy=false;return;}
    BiometricPrompt.Builder builder=new BiometricPrompt.Builder(this)
      .setTitle(mode.equals("enroll")?"Zenvora • Enable quick unlock":"Zenvora Pride • Owner login")
      .setSubtitle("Use your enrolled strong fingerprint or face")
      .setDescription("Secures your encrypted local business records")
      .setNegativeButton("Use passphrase",getMainExecutor(),(DialogInterface dialog,int which)->{
        busy=false;status("Use your owner passphrase instead.");
      });
    if(Build.VERSION.SDK_INT>=30)builder.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG);
    BiometricPrompt dialog=builder.build();
    dialog.authenticate(new BiometricPrompt.CryptoObject(cipher),new CancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
      @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
        busy=false;
        try{
          Cipher approved=result.getCryptoObject().getCipher();
          if(approved==null)throw new IllegalStateException("Protected cipher not available.");
          if(mode.equals("enroll")){
            byte[] secret=approved.doFinal(password.getBytes(StandardCharsets.UTF_8));
            store().edit().putString(P_WRAPPED,Base64.encodeToString(secret,Base64.NO_WRAP))
              .putString(P_IV,Base64.encodeToString(approved.getIV(),Base64.NO_WRAP))
              .putString(P_SALT,salt).apply();
            callJS("onBiometricEnrollment","true");
            status("Fingerprint / strong face login enabled. Your passphrase is protected by Android Keystore.");
          }else{
            byte[] plaintext=approved.doFinal(Base64.decode(store().getString(P_WRAPPED,""),Base64.NO_WRAP));
            String recovered=new String(plaintext,StandardCharsets.UTF_8);
            callJS("onBiometricSuccess",recovered);
            // No recovered passphrase is stored in a UI field or printed to logs.
          }
        }catch(Exception e){status("Device credential unavailable or invalidated. Unlock using your passphrase and re-enroll.");}
      }
      @Override public void onAuthenticationError(int code,CharSequence message){
        busy=false;status("Biometric sign-in unavailable: "+message+". Use your passphrase.");
      }
      @Override public void onAuthenticationFailed(){status("Biometric not recognized; try again or use passphrase.");}
    });
  }
  public class NativeAuth {
    @JavascriptInterface public void capabilities(){runOnUiThread(()->{
      try{
        if(Build.VERSION.SDK_INT<28){callJS("onStatus","Biometric quick unlock needs Android 9 or newer. Use your passphrase.");callJS("onDeviceReadiness","Android version too old for native quick unlock.");return;}
        if(Build.VERSION.SDK_INT>=30){
          android.hardware.biometrics.BiometricManager manager=getSystemService(android.hardware.biometrics.BiometricManager.class);
          int result=manager==null?-1:manager.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG);
          switch(result){
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS:
              callJS("onDeviceReadiness","Strong fingerprint / face biometric is available. Unlock your vault and enable quick unlock.");return;
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
              callJS("onDeviceReadiness","No strong fingerprint / face enrolled. Set up a fingerprint in Android Settings first.");return;
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
              callJS("onDeviceReadiness","No compatible strong biometric hardware. Password login remains available.");return;
            default:callJS("onDeviceReadiness","Strong biometrics unavailable or temporarily blocked. Use your passphrase.");return;
          }
        }
        if(Build.VERSION.SDK_INT==29){
          android.hardware.biometrics.BiometricManager manager=getSystemService(android.hardware.biometrics.BiometricManager.class);
          int result=manager==null?-1:manager.canAuthenticate();
          callJS("onDeviceReadiness",result==android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS?
            "Device biometrics appear available. Native prompt will still require cryptographic-grade authentication.":
            "Device biometrics unavailable or not enrolled. Use your passphrase.");return;
        }
        android.hardware.fingerprint.FingerprintManager manager=getSystemService(android.hardware.fingerprint.FingerprintManager.class);
        boolean ready=manager!=null&&manager.isHardwareDetected()&&manager.hasEnrolledFingerprints();
        callJS("onDeviceReadiness",ready?"Enrolled fingerprint detected. Native prompt will verify security level.":"No enrolled fingerprint detected. Use your passphrase.");
      }catch(Exception e){callJS("onDeviceReadiness","Could not verify biometrics. Your passphrase still works.");}
    });}

    @JavascriptInterface public void enroll(String pass,String salt){
      runOnUiThread(()->{
        if(busy)return;
        if(pass==null||pass.length()<12||pass.length()>128||salt==null||salt.length()>100||salt.length()<10){status("Check your passphrase and vault before enabling biometric login.");return;}
        if(Build.VERSION.SDK_INT<28){status("Android 9+ required for biometric login. Password remains available.");return;}
        busy=true;
        try{
          wipe();
          Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
          cipher.init(Cipher.ENCRYPT_MODE,newKey());
          prompt(cipher,"enroll",pass,salt);
        }catch(Exception e){busy=false;status("Strong biometric authentication is not available on this device. Use your passphrase.");}
      });
    }
    @JavascriptInterface public void unlock(String salt){
      runOnUiThread(()->{
        if(busy)return;
        if(salt==null||!salt.equals(store().getString(P_SALT,""))||store().getString(P_WRAPPED,null)==null){
          status("Quick unlock not set up for this vault. Enter your passphrase to enable it.");return;
        }
        busy=true;
        try{
          if(Build.VERSION.SDK_INT<28)throw new Exception("Old Android version");
          byte[] iv=Base64.decode(store().getString(P_IV,""),Base64.NO_WRAP);
          Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
          cipher.init(Cipher.DECRYPT_MODE,currentKey(),new GCMParameterSpec(128,iv));
          prompt(cipher,"unlock",null,salt);
        }catch(Exception e){busy=false;status("Biometric key missing, outdated or invalidated. Use your passphrase and re-enroll.");}
      });
    }
    @JavascriptInterface public void disable(){runOnUiThread(()->{wipe();status("Device biometric quick unlock disabled. Password login is still available.");});}
    @JavascriptInterface public void voiceCommand(){runOnUiThread(()->{
      Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");
      intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
      intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say: open owner login • or: use fingerprint. Do not speak passwords.");
      try{startActivityForResult(intent,VOICE_REQ);}catch(Exception e){status("Voice commands not available on this phone. Use your password or fingerprint.");}
    });}
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=VOICE_REQ)return;
    if(resultCode!=RESULT_OK||data==null){status("Voice command cancelled. Use your password or biometrics.");return;}
    ArrayList<String> results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    String said=(results==null||results.isEmpty())?"":results.get(0).toLowerCase(java.util.Locale.ROOT);
    if(said.contains("fingerprint")||said.contains("face unlock")||said.contains("use biometric"))callJS("onVoiceCommand","use_biometrics");
    else if(said.contains("open")||said.contains("login")||said.contains("sign in")||said.contains("vault"))callJS("onVoiceCommand","open_login");
    else callJS("onVoiceCommand","unknown");
  }

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    WebView.setWebContentsDebuggingEnabled(false);
    FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(6,29,23));
    webView=new WebView(this);webView.setBackgroundColor(Color.rgb(6,29,23));webView.setVisibility(View.INVISIBLE);
    WebSettings settings=webView.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
    settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setGeolocationEnabled(false);settings.setMediaPlaybackRequiresUserGesture(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);
    settings.setSavePassword(false);
    webView.setWebChromeClient(new WebChromeClient());
    CookieManager.getInstance().setAcceptCookie(false);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);
    webView.addJavascriptInterface(new NativeAuth(),"AndroidOwnerAuth");
    WebViewAssetLoader loader=new WebViewAssetLoader.Builder()
      .addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
    webView.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
        Uri u=r.getUrl();
        if("https".equalsIgnoreCase(u.getScheme())&&"appassets.androidplatform.net".equalsIgnoreCase(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/assets/"))return loader.shouldInterceptRequest(u);
        return new WebResourceResponse("text/plain","UTF-8",403,"Forbidden", Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
        Uri u=r.getUrl();
        if("https".equalsIgnoreCase(u.getScheme())&&"appassets.androidplatform.net".equalsIgnoreCase(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/assets/"))return false;
        if("https".equalsIgnoreCase(u.getScheme())&&!"appassets.androidplatform.net".equalsIgnoreCase(u.getHost())){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}}
        return true;
      }
      @Override public void onPageFinished(WebView v,String url){
        webView.setVisibility(View.VISIBLE);
        if(splash!=null){((ViewGroup)splash.getParent()).removeView(splash);splash=null;}
      }
    });
    root.addView(webView,new FrameLayout.LayoutParams(-1,-1));
    splash=new FrameLayout(this);splash.setBackgroundColor(Color.rgb(7,35,26));
    LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER);
    int pad=(int)(24*getResources().getDisplayMetrics().density);center.setPadding(pad,pad,pad,pad);
    ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.zenvora_brand);
    int iconSize=(int)(134*getResources().getDisplayMetrics().density);
    center.addView(icon,new LinearLayout.LayoutParams(iconSize,iconSize));
    TextView name=new TextView(this);name.setText("ZENVORA");name.setTextColor(Color.rgb(242,215,154));
    name.setTextSize(30);name.setTypeface(Typeface.SERIF,Typeface.BOLD);name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=pad;center.addView(name,lp);
    TextView tag=new TextView(this);tag.setText("EXPLORE  ·  PROTECT  ·  PROSPER\nZAMBIA KUCHALO");
    tag.setTextColor(Color.rgb(211,228,195));tag.setTextSize(12);tag.setGravity(Gravity.CENTER);
    center.addView(tag,new LinearLayout.LayoutParams(-1,-2));
    splash.addView(center,new FrameLayout.LayoutParams(-1,-1));root.addView(splash,new FrameLayout.LayoutParams(-1,-1));
    setContentView(root);webView.loadUrl(BASE+"/assets/index.html");
  }
  @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(webView!=null){webView.destroy();webView=null;}super.onDestroy();}
}
